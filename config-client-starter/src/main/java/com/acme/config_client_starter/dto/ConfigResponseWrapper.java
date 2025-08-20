package com.acme.config_client_starter.dto;

public record ConfigResponseWrapper<T>(
        T data,
        String message,
        boolean success,
        int statusCode
) {}
