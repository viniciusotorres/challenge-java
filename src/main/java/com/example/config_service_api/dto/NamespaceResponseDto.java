package com.example.config_service_api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record NamespaceResponseDto(
    UUID id,
    String name,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
