package com.acme.config_client_starter.consumer;

import com.acme.config_client_starter.configuration.ConfigClientProperties;
import com.acme.config_client_starter.service.ConfigService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import reactor.core.Disposable;
import reactor.core.publisher.BufferOverflowStrategy;
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Consumer Kafka não bloqueante para receber updates de configuração em tempo real.
 * Usa Reactor Kafka para processamento reativo e não bloqueante.
 */
@Configuration
@ConditionalOnProperty(
        prefix = "acme.config.client.kafka",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class KafkaConfigConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConfigConsumer.class);

    private final ConfigService configService;
    private final ConfigClientProperties properties;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private KafkaReceiver<String, String> kafkaReceiver;
    private Flux<ReceiverRecord<String, String>> kafkaFlux;
    private Disposable kafkaSubscription;


    public KafkaConfigConsumer(ConfigService configService,
                               ConfigClientProperties properties,
                               ObjectMapper objectMapper) {
        this.configService = configService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void startConsumer() {
        if (!properties.getKafka().isEnabled()) {
            logger.info("Kafka consumer disabled");
            return;
        }

        try {
            Map<String, Object> consumerProps = buildConsumerProperties();
            ReceiverOptions<String, String> receiverOptions = ReceiverOptions
                    .<String, String>create(consumerProps)
                    .subscription(Collections.singleton(properties.getKafka().getTopic()))
                    .addAssignListener(partitions ->
                            logger.debug("Partitions assigned: {}", partitions))
                    .addRevokeListener(partitions ->
                            logger.debug("Partitions revoked: {}", partitions));

            kafkaReceiver = KafkaReceiver.create(receiverOptions);

            kafkaSubscription = kafkaReceiver.receive()
                    .onBackpressureBuffer(1000,
                            buffer -> logger.warn("Kafka buffer overflow, dropping records"),
                            BufferOverflowStrategy.DROP_OLDEST)
                    .doOnSubscribe(s -> {
                        isRunning.set(true);
                        logger.info("Kafka consumer started for topic: {}",
                                properties.getKafka().getTopic());
                    })
                    .doOnNext(record -> {
                        try {
                            processMessage(record);
                        } finally {
                            record.receiverOffset().acknowledge();
                        }
                    })
                    .doOnError(error -> logger.error("Kafka consumer error", error))
                    .onErrorContinue((error, item) -> {
                        logger.error("Error processing Kafka message, continuing...", error);
                    })
                    .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(5))
                            .maxBackoff(Duration.ofSeconds(30))
                            .jitter(0.5))
                    .subscribe();

        } catch (Exception e) {
            logger.error("Failed to start Kafka consumer", e);
        }
    }

    @PreDestroy
    public void stopConsumer() {
        if (isRunning.compareAndSet(true, false)) {
            if (kafkaSubscription != null && !kafkaSubscription.isDisposed()) {
                kafkaSubscription.dispose();
            }
            logger.info("Kafka consumer stopped");
        }
    }

    /**
     * Processa mensagem recebida do Kafka
     */
    private void processMessage(ReceiverRecord<String, String> record) {
        try {
            String message = record.value();
            logger.debug("Received Kafka message: {}", message);

            ConfigUpdateMessage updateMessage = objectMapper.readValue(message, ConfigUpdateMessage.class);

            if (shouldProcessMessage(updateMessage)) {
                configService.processConfigUpdate(
                        updateMessage.getApplication(),
                        updateMessage.getProfile(),
                        updateMessage.getKey(),
                        updateMessage.getValue()
                );

                logger.debug("Processed config update: {}:{}:{} = {}",
                        updateMessage.getApplication(),
                        updateMessage.getProfile(),
                        updateMessage.getKey(),
                        updateMessage.getValue());
            } else {
                logger.trace("Ignoring config update for different app/profile: {}:{}",
                        updateMessage.getApplication(), updateMessage.getProfile());
            }

            record.receiverOffset().acknowledge();

        } catch (JsonProcessingException e) {
            logger.error("Failed to parse Kafka message: {}", record.value(), e);
            record.receiverOffset().acknowledge();
        } catch (Exception e) {
            logger.error("Error processing Kafka message", e);
        }
    }

    /**
     * Verifica se deve processar a mensagem baseado no application/profile
     */
    private boolean shouldProcessMessage(ConfigUpdateMessage message) {
        boolean applicationMatch = properties.getApplication().equals(message.getApplication());

        boolean profileMatch = properties.getProfile().equals(message.getProfile()) ||
                "global".equals(message.getProfile());

        return applicationMatch && profileMatch;
    }

    /**
     * Constrói propriedades do consumer Kafka
     */
    private Map<String, Object> buildConsumerProperties() {
        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                properties.getKafka().getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG,
                properties.getKafka().getGroupId() + "-" + properties.getApplication());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS,
                StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS,
                StringDeserializer.class);

        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);

        // Adiciona propriedades customizadas do usuário
        if (properties.getKafka().getConsumerProperties() != null) {
            props.putAll(properties.getKafka().getConsumerProperties());
        }

        return props;
    }

    /**
     * Retorna status do consumer
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * DTO para mensagem de update de configuração via Kafka
     */
    public static class ConfigUpdateMessage {
        private String application;
        private String profile;
        private String key;
        private Object value;
        private String operation;
        private long timestamp;
        private Map<String, Object> metadata;

        public ConfigUpdateMessage() {
        }

        public ConfigUpdateMessage(String application, String profile, String key,
                                   Object value, String operation) {
            this.application = application;
            this.profile = profile;
            this.key = key;
            this.value = value;
            this.operation = operation;
            this.timestamp = System.currentTimeMillis();
            this.metadata = Map.of();
        }

        public String getApplication() {
            return application;
        }

        public void setApplication(String application) {
            this.application = application;
        }

        public String getProfile() {
            return profile;
        }

        public void setProfile(String profile) {
            this.profile = profile;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public String getOperation() {
            return operation;
        }

        public void setOperation(String operation) {
            this.operation = operation;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata != null ? metadata : Map.of();
        }

        @Override
        public String toString() {
            return String.format(
                    "ConfigUpdate{application='%s', profile='%s', key='%s', value=%s, op='%s', ts=%d}",
                    application, profile, key, value, operation, timestamp
            );
        }
    }
}