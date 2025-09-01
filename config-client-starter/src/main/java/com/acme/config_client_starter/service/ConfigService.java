package com.acme.config_client_starter.service;


import com.acme.config_client_starter.configuration.ConfigClientProperties;
import com.acme.config_client_starter.dto.*;
import com.acme.config_client_starter.cache.LocalConfigCache;
import com.acme.config_client_starter.event.ConfigUpdateEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Serviço principal para gerenciamento de configurações distribuídas.
 * Integra cache local, API REST e updates via Kafka.
 */
@Service
public class ConfigService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);

    private final LocalConfigCache cache;
    private final WebClient webClient;
    private final ConfigClientProperties properties;
    private final ApplicationEventPublisher eventPublisher;
    private final ConversionService conversionService;
    private final ObjectMapper objectMapper;

    public ConfigService(LocalConfigCache cache,
                         WebClient.Builder webClientBuilder,
                         ConfigClientProperties properties,
                         ApplicationEventPublisher eventPublisher,
                         ConversionService conversionService,
                         ObjectMapper objectMapper) {
        this.cache = cache;
        this.properties = properties;
        this.eventPublisher = eventPublisher;
        this.conversionService = conversionService;
        this.objectMapper = objectMapper;

        this.webClient = webClientBuilder
                .baseUrl(properties.getServer().getBaseUrl())
                .defaultHeaders(headers -> properties.getServer().getHeaders()
                        .forEach(headers::add))
                .build();


        cache.setRefreshStrategy("*", this::fetchFromServer);

        logger.info("ConfigService initialized for namespace='{}', environment='{}'",
                properties.getApplication(), properties.getProfile());
    }

    /**
     * Busca valor de configuração com fallback hierárquico:
     * 1. Cache local
     * 2. Servidor via HTTP
     * 3. Valor padrão
     */
    public <T> Optional<T> getValue(String key, Class<T> type) {
        return getValue(properties.getApplication(), properties.getProfile(), key, type);
    }

    /**
     * Busca valor de configuração para namespace e environment específicos
     */
    public <T> Optional<T> getValue(String namespace, String environment, String key, Class<T> type) {
        String fullKey = buildFullKey(namespace, environment, key);

        // 1. Tenta cache local primeiro
        Optional<T> cachedValue = cache.get(fullKey, type);
        if (cachedValue.isPresent()) {
            logger.debug("Config found in cache: {}={}", fullKey, cachedValue.get());
            return cachedValue;
        }

        // 2. Busca no servidor se não encontrou no cache
        try {
            T serverValue = fetchFromServerSync(namespace, environment, key, type);
            if (serverValue != null) {
                cache.put(fullKey, serverValue, Map.of(
                        "namespace", namespace,
                        "environment", environment,
                        "source", "server"
                ));
                logger.debug("Config fetched from server: {}={}", fullKey, serverValue);
                return Optional.of(serverValue);
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch config from server: {}", fullKey, e);
        }

        logger.debug("Config not found: {}", fullKey);
        return Optional.empty();
    }

    /**
     * Busca valor com fallback para valor padrão
     */
    public <T> T getValue(String key, Class<T> type, T defaultValue) {
        return getValue(key, type).orElse(defaultValue);
    }

    /**
     * Busca valor como String (mais comum)
     */
    public Optional<String> getString(String key) {
        return getValue(key, String.class);
    }

    /**
     * Busca valor String com default
     */
    public String getString(String key, String defaultValue) {
        return getValue(key, String.class, defaultValue);
    }

    /**
     * Busca todas as configurações de um environment
     */
    public CompletableFuture<Map<String, Object>> getAllConfigurations(String namespace, String environment) {
        return fetchAllFromServer(namespace, environment)
                .doOnSuccess(configs -> {
                    // Armazena todas no cache
                    configs.forEach((key, value) -> {
                        String fullKey = buildFullKey(namespace, environment, key);
                        cache.put(fullKey, value, Map.of(
                                "namespace", namespace,
                                "environment", environment,
                                "source", "bulk_fetch"
                        ));
                    });
                    logger.debug("Cached {} configurations for {}:{}",
                            configs.size(), namespace, environment);
                })
                .toFuture();
    }

    /**
     * Força refresh de uma configuração específica
     */
    public CompletableFuture<Optional<Object>> refreshConfiguration(String key) {
        return refreshConfiguration(properties.getApplication(), properties.getProfile(), key);
    }

    /**
     * Força refresh de uma configuração específica para namespace/environment
     */
    public CompletableFuture<Optional<Object>> refreshConfiguration(String namespace, String environment, String key) {
        String fullKey = buildFullKey(namespace, environment, key);
        return cache.refresh(fullKey);
    }

    /**
     * Processa update de configuração (chamado pelo Kafka consumer)
     */
    public void processConfigUpdate(String namespace, String environment, String key, Object newValue) {
        String fullKey = buildFullKey(namespace, environment, key);

        // Recupera valor anterior do cache
        Object oldValue = cache.get(fullKey).orElse(null);

        // Atualiza cache
        cache.put(fullKey, newValue, Map.of(
                "namespace", namespace,
                "environment", environment,
                "source", "kafka_update",
                "updatedAt", System.currentTimeMillis()
        ));


        ConfigUpdateEvent event = new ConfigUpdateEvent(
                this, namespace, environment, key, oldValue, newValue
        );
        eventPublisher.publishEvent(event);

        logger.info("Config updated via Kafka: {}={} (was={})", fullKey, newValue, oldValue);
    }

    /**
     * Converte valor para tipo específico
     */
    @SuppressWarnings("unchecked")
    public <T> T convertValue(Object value, Class<T> targetType) {
        if (value == null) {
            return null;
        }

        if (targetType.isAssignableFrom(value.getClass())) {
            return (T) value;
        }

        if (conversionService.canConvert(value.getClass(), targetType)) {
            return conversionService.convert(value, targetType);
        }

        try {
            return objectMapper.convertValue(value, targetType);
        } catch (Exception e) {
            logger.warn("Failed to convert value '{}' to type {}", value, targetType.getSimpleName(), e);
            throw new IllegalArgumentException("Cannot convert value to " + targetType.getSimpleName(), e);
        }
    }

    /**
     * Constrói chave completa no formato namespace:environment:key
     */
    private String buildFullKey(String namespace, String environment, String key) {
        return String.format("%s:%s:%s", namespace, environment, key);
    }

    /**
     * Busca configuração do servidor de forma síncrona
     */
    private <T> T fetchFromServerSync(String namespace, String environment, String key, Class<T> type) {
        try {
            String url = String.format("/data-config/%s/%s/configs/%s", namespace, environment, key);

            ApiResponseDto response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(ApiResponseDto.class)
                    .timeout(properties.getServer().getTimeout())
                    .retryWhen(Retry.backoff(
                            properties.getResilience().getMaxRetries(),
                            properties.getResilience().getRetryDelay()
                    ))
                    .block();

            if (response != null && response.success() && response.data() != null) {
                if (response.data() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataMap = (Map<String, Object>) response.data();
                    Object value = dataMap.get("value");
                    if (value != null) {
                        return convertValue(value, type);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch config {}:{}:{}", namespace, environment, key, e);
        }

        return null;
    }

    /**
     * Busca configuração do servidor (usado pelo cache refresh)
     */
    private Object fetchFromServer(String fullKey) {
        String[] parts = fullKey.split(":", 3);
        if (parts.length != 3) {
            logger.warn("Invalid key format for server fetch: {}", fullKey);
            return null;
        }

        return fetchFromServerSync(parts[0], parts[1], parts[2], Object.class);
    }

    /**
     * Busca todas as configurações de um environment
     */
    private Mono<Map<String, Object>> fetchAllFromServer(String namespace, String environment) {
        String url = String.format("/data-config/%s/%s/configs", namespace, environment);

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(ApiResponseDto.class)
                .timeout(properties.getServer().getTimeout())
                .retryWhen(Retry.backoff(
                        properties.getResilience().getMaxRetries(),
                        properties.getResilience().getRetryDelay()
                ))
                .map(this::extractConfigsFromPageableResponse);
    }

    /**
     * Extrai configurações da resposta paginada da API
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractConfigsFromPageableResponse(ApiResponseDto response) {
        Map<String, Object> configs = new HashMap<>();

        if (response != null && response.success() && response.data() != null) {
            try {
                Map<String, Object> dataMap = (Map<String, Object>) response.data();
                List<Map<String, Object>> content = (List<Map<String, Object>>) dataMap.get("content");

                if (content != null) {
                    for (Map<String, Object> configItem : content) {
                        String key = (String) configItem.get("key");
                        Object value = configItem.get("value");
                        if (key != null && value != null) {
                            configs.put(key, value);
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to extract configs from API response", e);
            }
        }

        return configs;
    }


}