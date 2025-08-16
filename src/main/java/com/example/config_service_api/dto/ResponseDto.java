package com.example.config_service_api.dto;

import lombok.Builder;

import java.io.Serializable;

@Builder
public record ResponseDto<T>(T data, String message, boolean success, int statusCode) implements Serializable {
}
