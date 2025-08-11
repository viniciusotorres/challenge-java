package com.example.config_service_api.dto;

public record ResponseDto<T>(
    T data,
    String message,
    int statusCode
) {
}
