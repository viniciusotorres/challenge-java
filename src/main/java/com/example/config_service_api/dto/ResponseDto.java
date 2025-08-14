package com.example.config_service_api.dto;

import lombok.Builder;

@Builder
public record ResponseDto<T>(T data, String message, boolean success, int statusCode) {
}
