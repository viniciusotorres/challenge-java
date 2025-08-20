package com.example.config_service_api.config;

import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {


    // => Url do broker
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // => Tentativas caso falhe ao enviar mensagem (default)
    @Value("${kafka.producer.retries:3}")
    private Integer retries;

    // => Tamanho em bytes do lote de mensagem antes de enviar ao broker
    @Value("${kafka.producer.batch-size:16384}")
    private Integer batchSize;

    // => Tempo que o producer espera antes de enviar o batch
    @Value("${kafka.producer.linger-ms:1}")
    private Integer lingerMs;

    // => Mémoria todal para amazenar antes de serem enviadas. (default)
    @Value("${kafka.producer.buffer-memory:33554432}")
    private Long bufferMemory;

    @Bean
    public ProducerFactory<String, ConfigChangeEvent> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        configProps.put(ProducerConfig.RETRIES_CONFIG, retries);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);

        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, ConfigChangeEvent> kafkaTemplate() {
       return new KafkaTemplate<>(producerFactory());
    }
}