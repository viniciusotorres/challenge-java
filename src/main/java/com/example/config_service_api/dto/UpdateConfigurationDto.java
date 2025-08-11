package com.example.config_service_api.dto;

public record UpdateConfigurationDto(
    String key,
    String value,
    CreateNamespaceDto namespace,
    CreateEnvironmentDto environment
) {
}
