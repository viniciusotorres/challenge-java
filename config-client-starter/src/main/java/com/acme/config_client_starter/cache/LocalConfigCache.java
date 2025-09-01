package com.acme.config_client_starter.cache;

import com.acme.config_client_starter.configuration.ConfigClientProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Cache local para configurações com TTL, refresh automático e estratégias de atualização.
 */
@Component
public class LocalConfigCache {

    private static final Logger logger = LoggerFactory.getLogger(LocalConfigCache.class);

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Map<String, Function<String, Object>> refreshStrategies = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private final ConfigClientProperties properties;
    private final ConversionService conversionService;
    private final ObjectMapper objectMapper;

    public LocalConfigCache(ConfigClientProperties properties,
                            ConversionService conversionService,
                            ObjectMapper objectMapper) {
        this.properties = properties;
        this.conversionService = conversionService;
        this.objectMapper = objectMapper;

        // Inicia limpeza periódica de entradas expiradas
        startPeriodicCleanup();
    }

    /**
     * Recupera valor do cache com conversão de tipo
     */
    public <T> Optional<T> get(String key, Class<T> type) {
        CacheEntry entry = cache.get(key);

        if (entry == null || entry.isExpired()) {
            if (entry != null) {
                logger.debug("Cache entry expired for key: {}", key);
                cache.remove(key);
            }
            return Optional.empty();
        }

        try {
            T convertedValue = convertValue(entry.getValue(), type);
            entry.updateLastAccessed();
            logger.debug("Cache hit for key: {} -> {}", key, convertedValue);
            return Optional.of(convertedValue);
        } catch (Exception e) {
            logger.warn("Failed to convert cached value for key '{}' to type {}", key, type.getSimpleName(), e);
            return Optional.empty();
        }
    }

    /**
     * Recupera valor do cache sem conversão de tipo
     */
    public Optional<Object> get(String key) {
        return get(key, Object.class);
    }

    /**
     * Armazena valor no cache com metadados
     */
    public void put(String key, Object value, Map<String, Object> metadata) {
        if (value == null) {
            logger.debug("Skipping null value for key: {}", key);
            return;
        }

        long ttlMillis = properties.getCache().getTtl().toMillis();
        CacheEntry entry = new CacheEntry(value, ttlMillis, metadata);
        cache.put(key, entry);

        logger.debug("Cached value for key: {} (TTL: {}ms)", key, ttlMillis);
    }

    /**
     * Armazena valor no cache sem metadados
     */
    public void put(String key, Object value) {
        put(key, value, Map.of("source", "manual"));
    }

    /**
     * Remove entrada do cache
     */
    public void evict(String key) {
        CacheEntry removed = cache.remove(key);
        if (removed != null) {
            logger.debug("Evicted cache entry for key: {}", key);
        }
    }

    /**
     * Limpa todo o cache
     */
    public void clear() {
        int size = cache.size();
        cache.clear();
        logger.info("Cleared cache ({} entries)", size);
    }

    /**
     * Define estratégia de refresh para chaves que correspondem ao padrão
     */
    public void setRefreshStrategy(String keyPattern, Function<String, Object> refreshFunction) {
        refreshStrategies.put(keyPattern, refreshFunction);
        logger.debug("Set refresh strategy for pattern: {}", keyPattern);
    }

    /**
     * Força refresh de uma entrada usando a estratégia configurada
     */
    public CompletableFuture<Optional<Object>> refresh(String key) {
        return CompletableFuture.supplyAsync(() -> {
            Function<String, Object> refreshStrategy = findRefreshStrategy(key);

            if (refreshStrategy == null) {
                logger.debug("No refresh strategy found for key: {}", key);
                return Optional.empty();
            }

            try {
                Object newValue = refreshStrategy.apply(key);
                if (newValue != null) {
                    // Preserva metadados existentes se houver
                    Map<String, Object> metadata = Optional.ofNullable(cache.get(key))
                            .map(CacheEntry::getMetadata)
                            .orElse(Map.of());

                    // Adiciona timestamp do refresh
                    Map<String, Object> updatedMetadata = new ConcurrentHashMap<>(metadata);
                    updatedMetadata.put("lastRefresh", System.currentTimeMillis());
                    updatedMetadata.put("source", "refresh");

                    put(key, newValue, updatedMetadata);
                    logger.debug("Refreshed cache entry for key: {} -> {}", key, newValue);
                    return Optional.of(newValue);
                }
            } catch (Exception e) {
                logger.warn("Failed to refresh cache entry for key: {}", key, e);
            }

            return Optional.empty();
        });
    }

    /**
     * Verifica se uma chave existe no cache e não está expirada
     */
    public boolean contains(String key) {
        CacheEntry entry = cache.get(key);
        return entry != null && !entry.isExpired();
    }

    /**
     * Retorna estatísticas do cache
     */
    public CacheStats getStats() {
        int totalEntries = cache.size();
        int expiredEntries = (int) cache.values().stream()
                .mapToInt(entry -> entry.isExpired() ? 1 : 0)
                .sum();

        return new CacheStats(totalEntries, totalEntries - expiredEntries, expiredEntries);
    }

    /**
     * Encontra estratégia de refresh para uma chave
     */
    private Function<String, Object> findRefreshStrategy(String key) {
        if (refreshStrategies.containsKey(key)) {
            return refreshStrategies.get(key);
        }

        for (Map.Entry<String, Function<String, Object>> entry : refreshStrategies.entrySet()) {
            String pattern = entry.getKey();
            if (pattern.equals("*") || matchesPattern(key, pattern)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Verifica se chave corresponde ao padrão (suporte básico para *)
     */
    private boolean matchesPattern(String key, String pattern) {
        if (pattern.equals("*")) {
            return true;
        }

        if (pattern.endsWith("*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return key.startsWith(prefix);
        }

        if (pattern.startsWith("*")) {
            String suffix = pattern.substring(1);
            return key.endsWith(suffix);
        }

        return key.equals(pattern);
    }

    /**
     * Converte valor para tipo específico usando ConversionService e Jackson
     */
    @SuppressWarnings("unchecked")
    private <T> T convertValue(Object value, Class<T> targetType) {
        if (value == null) {
            return null;
        }

        if (targetType.isAssignableFrom(value.getClass())) {
            return (T) value;
        }

        // Usa ConversionService do Spring
        if (conversionService.canConvert(value.getClass(), targetType)) {
            return conversionService.convert(value, targetType);
        }

        // Fallback para Jackson
        try {
            return objectMapper.convertValue(value, targetType);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot convert value to " + targetType.getSimpleName(), e);
        }
    }

    /**
     * Inicia limpeza periódica de entradas expiradas
     */
    private void startPeriodicCleanup() {
        long cleanupIntervalMinutes = 5; // Cleanup a cada 5 minutos

        scheduler.scheduleAtFixedRate(() -> {
            try {
                int cleaned = cleanupExpiredEntries();
                if (cleaned > 0) {
                    logger.debug("Cleaned up {} expired cache entries", cleaned);
                }
            } catch (Exception e) {
                logger.warn("Error during cache cleanup", e);
            }
        }, cleanupIntervalMinutes, cleanupIntervalMinutes, TimeUnit.MINUTES);
    }

    /**
     * Remove entradas expiradas do cache
     */
    private int cleanupExpiredEntries() {
        int cleaned = 0;
        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            if (entry.getValue().isExpired()) {
                cache.remove(entry.getKey());
                cleaned++;
            }
        }
        return cleaned;
    }

    /**
     * Entrada do cache com TTL e metadados
     */
    private static class CacheEntry {
        private final Object value;
        private final long expiresAt;
        private final Map<String, Object> metadata;
        private volatile long lastAccessed;

        public CacheEntry(Object value, long ttlMillis, Map<String, Object> metadata) {
            this.value = value;
            this.expiresAt = System.currentTimeMillis() + ttlMillis;
            this.metadata = new ConcurrentHashMap<>(metadata);
            this.lastAccessed = System.currentTimeMillis();
        }

        public Object getValue() {
            return value;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }

        public void updateLastAccessed() {
            this.lastAccessed = System.currentTimeMillis();
        }

        public long getLastAccessed() {
            return lastAccessed;
        }

        public long getExpiresAt() {
            return expiresAt;
        }
    }

    /**
     * Estatísticas do cache
     */
    public static class CacheStats {
        private final int totalEntries;
        private final int validEntries;
        private final int expiredEntries;

        public CacheStats(int totalEntries, int validEntries, int expiredEntries) {
            this.totalEntries = totalEntries;
            this.validEntries = validEntries;
            this.expiredEntries = expiredEntries;
        }

        public int getTotalEntries() { return totalEntries; }
        public int getValidEntries() { return validEntries; }
        public int getExpiredEntries() { return expiredEntries; }

        @Override
        public String toString() {
            return String.format("CacheStats{total=%d, valid=%d, expired=%d}",
                    totalEntries, validEntries, expiredEntries);
        }
    }
}