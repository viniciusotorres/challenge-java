package com.acme.config_client_starter.dto;

public record ConfigResponseDto (
    String id,
    String key,
    String value,
    String environmentId,
    String environmentName
){
}
