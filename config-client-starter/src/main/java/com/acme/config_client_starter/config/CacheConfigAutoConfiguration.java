// Java
package com.acme.config_client_starter.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuração do cache para o Config Client
 */
@AutoConfiguration
@ConditionalOnClass(CacheManager.class)
@ConditionalOnProperty(prefix = "acme.config-client", name = "cache-enabled", havingValue = "true", matchIfMissing = true)
@EnableCaching
public class CacheConfigAutoConfiguration {

    /**
     * Configura o cache manager para as configurações
     */
    @Bean("configCacheManager")
    public CacheManager configCacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(java.util.Collections.singletonList("config-cache"));
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }
}