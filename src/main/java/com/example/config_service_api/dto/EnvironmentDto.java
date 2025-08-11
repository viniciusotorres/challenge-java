package com.example.config_service_api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record EnvironmentDto(
        UUID id,
        String name) {
}
