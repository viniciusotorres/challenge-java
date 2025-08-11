package com.example.config_service_api.dto;

import com.example.config_service_api.entity.Environment;
import com.example.config_service_api.entity.Namespace;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConfigurationDto(
    UUID id,
    String key,
    String value,
    EnvironmentDto environment,
    NamespaceDto namespace,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

}
