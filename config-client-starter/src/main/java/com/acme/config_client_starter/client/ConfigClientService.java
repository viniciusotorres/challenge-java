package com.acme.config_client_starter.client;

import org.springframework.http.*;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.print.Pageable;
import java.util.Map;
import java.util.Optional;

@Service
public class ConfigClientService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigClientService.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ConfigClientService() {
        this(new RestTemplate(), "http://localhost:8080", "user", "user123");
    }

    public ConfigClientService(RestTemplate restTemplate) {
        this(restTemplate, "http://localhost:8080", "user", "user123");
    }

    public ConfigClientService(RestTemplate restTemplate, String baseUrl, String username, String password) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        this.restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(username, password));

        logger.info("ConfigClientService initialized with base URL: {}", this.baseUrl);
    }

    /**
     * Cria uma nova configuração - POST /data-config/create
     */
    public Optional<Map<String, Object>> createConfig(String namespace, String environment, String key, String value) {
        try {
            String url = baseUrl + "/data-config/create";

            Map<String, Object> request = Map.of(
                    "namespace", namespace,
                    "environment", environment,
                    "key", key,
                    "value", value
            );

            logger.debug("Creating config: {}={} for {}/{}", key, value, namespace, environment);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url,
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.of(response.getBody());
            }

            return Optional.empty();

        } catch (RestClientException e) {
            logger.error("Error creating configuration {}/{}: {}", namespace, environment, key, e);
            throw new RuntimeException("Failed to create configuration", e);
        }
    }

    /**
     * Lista configurações - GET /data-config/{namespace}/{environment}/configs
     */
    public Optional<Map<String, Object>> listConfigs(String namespace, String environment) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path("/data-config/{namespace}/{environment}/configs")
                    .buildAndExpand(namespace, environment)
                    .toUriString();

            logger.debug("Listing configs for {}/{}", namespace, environment);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.of(response.getBody());
            }

            return Optional.empty();

        } catch (RestClientException e) {
            logger.error("Error listing configurations for {}/{}", namespace, environment, e);
            throw new RuntimeException("Failed to list configurations", e);
        }
    }

    /**
     * Atualiza configuração - PUT /data-config/{namespace}/{environment}/configs/{key}
     */
    public Optional<Map<String, Object>> updateConfig(String namespace, String environment, String key, String value) {
        try {
            String url = baseUrl + "/data-config/{namespace}/{environment}/configs/{key}";

            logger.debug("Updating config {}/{}: {} -> {}", namespace, environment, key, value);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> requestEntity = new HttpEntity<>(value, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    requestEntity,
                    Map.class,
                    namespace, environment, key
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.of(response.getBody());
            }

            return Optional.empty();

        } catch (RestClientException e) {
            logger.error("Error updating configuration {}/{}: {}", namespace, environment, key, e);
            throw new RuntimeException("Failed to update configuration", e);
        }
    }

    /**
     * Obtém configuração específica - GET /data-config/{namespace}/{environment}/configs/{key}
     */
    public Optional<Map<String, Object>> getConfig(String namespace, String environment, String key) {
        try {
            String url = baseUrl + "/data-config/{namespace}/{environment}/configs/{key}";

            logger.debug("Getting config {}/{}: {}", namespace, environment, key);

            ResponseEntity<Map> response = restTemplate.getForEntity(
                    url,
                    Map.class,
                    namespace, environment, key
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.of(response.getBody());
            }

            return Optional.empty();

        } catch (HttpClientErrorException.NotFound e) {
            logger.debug("Configuration not found: {}/{}: {}", namespace, environment, key);
            return Optional.empty();
        } catch (RestClientException e) {
            logger.error("Error getting configuration {}/{}: {}", namespace, environment, key, e);
            throw new RuntimeException("Failed to get configuration", e);
        }
    }

    /**
     * Deleta configuração - DELETE /data-config/{namespace}/{environment}/configs/{key}
     */
    public boolean deleteConfig(String namespace, String environment, String key) {
        try {
            String url = baseUrl + "/data-config/{namespace}/{environment}/configs/{key}";

            logger.debug("Deleting config {}/{}: {}", namespace, environment, key);

            restTemplate.delete(url, namespace, environment, key);
            return true;

        } catch (RestClientException e) {
            logger.error("Error deleting configuration {}/{}: {}", namespace, environment, key, e);
            throw new RuntimeException("Failed to delete configuration", e);
        }
    }

    /**
     * Obtém histórico - GET /data-config/{namespace}/{environment}/history
     */
    public Optional<Map<String, Object>> getHistory(String namespace, String environment, Pageable pageable) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path("/data-config/{namespace}/{environment}/history")
                    .buildAndExpand(namespace, environment)
                    .toUriString();

            logger.debug("Getting history for {}/{}", namespace, environment);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.of(response.getBody());
            }

            return Optional.empty();

        } catch (RestClientException e) {
            logger.error("Error getting history for {}/{}", namespace, environment, e);
            throw new RuntimeException("Failed to get history", e);
        }
    }

    /**
     * Método simplificado para buscar apenas o valor
     */
    public Optional<String> getConfigValue(String namespace, String environment, String key) {
        Optional<Map<String, Object>> config = getConfig(namespace, environment, key);
        return config.map(c -> c.get("value").toString());
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public boolean isServiceAvailable() {
        try {
            String url = baseUrl + "/actuator/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            logger.warn("Config service is not available at: {}", baseUrl);
            return false;
        }
    }
}