package com.example.config_service_api.dto;

import java.util.UUID;

public record NamespaceDto(
        UUID id,
        String name
) {
}
