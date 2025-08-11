package com.example.config_service_api.dto;

public record ErrorResponseDto(
    String message,
    int statusCode
) {
}
