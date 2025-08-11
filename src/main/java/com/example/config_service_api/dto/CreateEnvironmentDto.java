package com.example.config_service_api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateEnvironmentDto(
        @NotBlank(message = "Name não pode ser vazio")
        String name
) {
}
