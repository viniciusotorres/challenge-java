package com.acme.config_client_starter.dto;

public record DataConfigResponseDto(
         String key,
         String value,
         String namespace,
         String environment,
         String createdAt,
         String updatedAt
) {
}
