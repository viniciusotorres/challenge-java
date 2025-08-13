package com.example.config_service_api.dto;

import lombok.Builder;

@Builder
public record NamespaceUpdateDto(
        String name
) {
}
