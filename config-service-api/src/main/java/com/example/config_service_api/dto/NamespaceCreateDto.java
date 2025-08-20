package com.example.config_service_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NamespaceCreateDto(
        @NotBlank(message = "O nome do namespace é obrigatório")
        @Size(max = 100, message = "O nome do namespace deve ter no máximo 100 caracteres")
        String name
) {
}
