package com.example.config_service_api.dto;
import jakarta.validation.constraints.NotBlank;

public record CreateConfigurationDto(
        @NotBlank (message = "Namespace não pode estar vazio")
        CreateNamespaceDto namespace,
        @NotBlank (message = "Environment não pode estar vazio")
        CreateEnvironmentDto environment,
        @NotBlank (message = "Key não pode estar vazia")
        String key,
        @NotBlank (message = "Value não pode estar vazio")
        String value
) {
}
