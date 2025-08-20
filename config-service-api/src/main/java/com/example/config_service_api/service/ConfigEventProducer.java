package com.example.config_service_api.service;

import com.example.config_service_api.config.ConfigChangeEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ConfigEventProducer {

    private final KafkaTemplate<String, ConfigChangeEvent> template;

    public ConfigEventProducer(KafkaTemplate<String, ConfigChangeEvent> template) {
        this.template = template;
    }

    public void sendConfigChange(ConfigChangeEvent event) {
        String key = event.namespace() + ":" + event.environment()  + ":"  + event.key();
        template.send("config-changes", key, event);
    }
}
