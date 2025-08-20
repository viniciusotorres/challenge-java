package com.example.config_service_api.config;

import java.time.Instant;
import java.util.UUID;

public record ConfigChangeEvent(
        String action,
        UUID configId,
        String namespace,
        String environment,
        String key,
        String value,
        Instant timestamp,
        String changedBy
) {
    public ConfigChangeEvent {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public String getEventKey() {
        return namespace + ":" + environment + ":" + key;
    }
}