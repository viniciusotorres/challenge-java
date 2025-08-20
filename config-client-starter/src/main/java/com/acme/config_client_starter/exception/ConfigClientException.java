package com.acme.config_client_starter.exception;

/**
 * Exceção personalizada para erros do ConfigClientService
 */
public class ConfigClientException extends RuntimeException {

    public ConfigClientException(String message) {
        super(message);
    }

    public ConfigClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
