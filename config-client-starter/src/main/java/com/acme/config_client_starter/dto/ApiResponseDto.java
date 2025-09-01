package com.acme.config_client_starter.dto;

public record ApiResponseDto(
        boolean success,
        int statusCode,
        String message,
        Object data,
        Long timestamp
) {
}

