package com.example.config_service_api.service;

import com.example.config_service_api.config.ConfigChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@Service
public class ConfigEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(ConfigEventProducer.class);
    private static final String TOPIC_NAME = "config-changes";

    private final KafkaTemplate<String, ConfigChangeEvent> kafkaTemplate;

    public ConfigEventProducer(KafkaTemplate<String, ConfigChangeEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Envia evento de forma assíncrona com callback padrão
     */
    @Async
    @TransactionalEventListener
    public void sendConfigChange(ConfigChangeEvent event) {
        String key = event.getEventKey();

        try {
            CompletableFuture<SendResult<String, ConfigChangeEvent>> future =
                    kafkaTemplate.send(TOPIC_NAME, key, event);

            future.whenComplete(createDefaultCallback(event));

            logger.debug("Evento enviado para Kafka - Key: {}, Action: {}", key, event.action());

        } catch (Exception e) {
            logger.error("Falha ao enviar evento para Kafka: {}", event, e);
            handleFailedEvent(event, e);
        }
    }

    /**
     * Envia evento com callback personalizado
     */
    public void sendConfigChange(ConfigChangeEvent event,
                                 BiConsumer<SendResult<String, ConfigChangeEvent>, Throwable> customCallback) {
        String key = event.getEventKey();

        try {
            CompletableFuture<SendResult<String, ConfigChangeEvent>> future =
                    kafkaTemplate.send(TOPIC_NAME, key, event);

            future.whenComplete(customCallback);

        } catch (Exception e) {
            logger.error("Falha ao enviar evento para Kafka: {}", event, e);
            handleFailedEvent(event, e);
        }
    }

    /**
     * Callback padrão para tratamento de resultados
     */
    @NonNull
    private BiConsumer<SendResult<String, ConfigChangeEvent>, Throwable> createDefaultCallback(
            ConfigChangeEvent event) {
        return (result, ex) -> {
            if (ex == null) {
                handleSuccess(event, result);
            } else {
                handleError(event, ex);
            }
        };
    }

    private void handleSuccess(ConfigChangeEvent event, SendResult<String, ConfigChangeEvent> result) {
        logger.info("Evento enviado com sucesso - Key: {}, Topic: {}, Partition: {}, Offset: {}",
                event.getEventKey(),
                result.getRecordMetadata().topic(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());

        // Aqui você pode adicionar métricas
        // metricsService.incrementSuccessCounter(event.namespace(), event.environment());
    }

    private void handleError(ConfigChangeEvent event, Throwable ex) {
        logger.error("Falha ao enviar evento - Key: {}, Erro: {}",
                event.getEventKey(), ex.getMessage(), ex);

        // Aqui você pode adicionar métricas de erro
        // metricsService.incrementErrorCounter(event.namespace(), event.environment());

        handleFailedEvent(event, ex);
    }

    /**
     * Fallback para eventos que falharam no envio
     */
    private void handleFailedEvent(ConfigChangeEvent event, Throwable cause) {
        try {
            logger.warn("Armazenando evento falho para reprocessamento: {}", event);
            // fallbackService.storeFailedEvent(event, cause);

        } catch (Exception storageEx) {
            logger.error("Falha ao armazenar evento para reprocessamento: {}", storageEx.getMessage(), storageEx);
        }
    }
}