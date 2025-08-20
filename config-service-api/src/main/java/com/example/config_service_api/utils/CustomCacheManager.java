package com.example.config_service_api.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Slf4j
public class CustomCacheManager {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public CustomCacheManager(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public <T> T getFromCache(String keyPattern, Class<T> type) {
        try {
            Set<String> keys = stringRedisTemplate.keys(keyPattern);
            if (keys != null && !keys.isEmpty()) {
                String key = keys.iterator().next();
                String json = stringRedisTemplate.opsForValue().get(key);

                if (json != null) {
                    return objectMapper.readValue(json, type);
                }
            }
        } catch (Exception e) {
            log.warn("Erro ao buscar do cache: {}", e.getMessage());
        }
        return null;
    }

    public void saveToCache(String key, Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            stringRedisTemplate.opsForValue().set(key, json);
            log.info("Dados salvos no cache. Key: {}", key);
        } catch (Exception e) {
            log.warn("Erro ao salvar no cache: {}", e.getMessage());
        }
    }

    public void deleteFromCache(String keyPattern) {
        try {
            Set<String> keys = stringRedisTemplate.keys(keyPattern);
            if (keys != null) {
                keys.forEach(stringRedisTemplate::delete);
                log.info("Cache invalidado para padrão: {}", keyPattern);
            }
        } catch (Exception e) {
            log.warn("Erro ao invalidar cache: {}", e.getMessage());
        }
    }
}
