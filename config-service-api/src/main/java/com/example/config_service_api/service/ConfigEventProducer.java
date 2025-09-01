package com.example.config_service_api.service;

import com.example.config_service_api.config.ConfigChangeEvent;
import com.example.config_service_api.config.KafkaProducerConfig;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ConfigEventProducer {

    private final KafkaTemplate<String, ConfigChangeEvent> template;
    private final String topicName;

    public ConfigEventProducer(KafkaTemplate<String, ConfigChangeEvent> template,
                               KafkaProducerConfig kafkaConfig) {
        this.template = template;
        this.topicName = kafkaConfig.getTopicName();
    }

    public void sendConfigChange(ConfigChangeEvent event) {
        String key = event.namespace() + ":" + event.environment() + ":" + event.key();
        template.send(topicName, key, event);
    }
}