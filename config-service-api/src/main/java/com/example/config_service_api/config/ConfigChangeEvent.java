package com.example.config_service_api.config;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConfigChangeEvent(
        String action,
        UUID configId,
        String namespace,
        String environment,
        String key,
        String value,
        LocalDateTime timestamp
) {
}
