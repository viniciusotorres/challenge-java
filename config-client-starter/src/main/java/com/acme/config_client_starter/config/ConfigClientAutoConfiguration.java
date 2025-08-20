// Java
package com.acme.config_client_starter.config;

import com.acme.config_client_starter.service.ConfigClientService;
import com.acme.config_client_starter.ConfigClientProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Auto-configuração principal do Config Client
 */
@AutoConfiguration
@EnableConfigurationProperties(ConfigClientProperties.class)
@ConditionalOnProperty(prefix = "acme.config-client", name = "enabled", matchIfMissing = true)
public class ConfigClientAutoConfiguration {

    /**
     * Cria o RestTemplate configurado para o Config Client
     */
    @Bean("configClientRestTemplate")
    @ConditionalOnMissingBean(name = "configClientRestTemplate")
    public RestTemplate configClientRestTemplate(ConfigClientProperties properties) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) properties.getConnectionTimeout());
        factory.setReadTimeout((int) properties.getReadTimeout());
        return new RestTemplateBuilder()
                .requestFactory(() -> factory)
                .build();
    }

    /**
     * Cria o serviço principal do Config Client
     */
    @Bean
    @ConditionalOnMissingBean
    public ConfigClientService configClientService(RestTemplate configClientRestTemplate,
                                                    ConfigClientProperties properties) {
        return new ConfigClientService(configClientRestTemplate, properties.getBaseUrl());
    }
}