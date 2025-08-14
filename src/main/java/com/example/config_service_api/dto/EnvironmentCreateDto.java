package com.example.config_service_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record EnvironmentCreateDto(
        @NotBlank(message = "O nome do ambiente é obrigatório")
        @Size(max = 100, message = "O nome do ambiente deve ter no máximo 100 caracteres")
        String name,
        @NotNull(message = "O namespaceId é obrigatório")
        UUID namespaceId
) {
}
