package com.example.config_service_api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateNamespaceDto(
        @NotBlank (message = "Name não pode estar vazio")
        String name
) {
}
