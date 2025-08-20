package com.acme.config_client_starter.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public record DataConfigResponseDto(
        UUID id,
        String key,
        String value,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        UUID environmentId
) implements Serializable {
}
