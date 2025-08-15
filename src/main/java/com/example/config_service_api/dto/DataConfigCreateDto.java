package com.example.config_service_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.UUID;

@Builder
public record DataConfigCreateDto(
        @NotBlank(message = "Key é obrigatória")
        @Size(max = 120, message = "Key deve ter no máximo 120 caracteres")
        String key,
        @NotBlank(message = "Value é obrigatória")
        String value,
        @NotNull(message = "EnvironmentId é obrigatório")
        UUID environmentId
) {
}
