package com.acme.config_client_starter.dto;

public record ConfigRequestDto(
    String key,
    String value
) {
}
