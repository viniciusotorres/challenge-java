package com.example.config_service_api.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record EnvironmentResponseDto(
    UUID id,
    String name,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    NamespaceResponseDto namespace
) {
}
