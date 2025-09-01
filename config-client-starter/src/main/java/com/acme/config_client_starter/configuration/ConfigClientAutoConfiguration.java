package com.acme.config_client_starter.configuration;

import com.acme.config_client_starter.processor.ConfigValuePostProcessor;
import com.acme.config_client_starter.cache.LocalConfigCache;
import com.acme.config_client_starter.consumer.KafkaConfigConsumer;
import com.acme.config_client_starter.service.ConfigService;
import com.acme.config_client_starter.sync.ConfigSyncJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Auto-configuração do Spring Boot para o Config Client.
 * Configura automaticamente todos os componentes necessários.
 */
@AutoConfiguration
@EnableConfigurationProperties(ConfigClientProperties.class)
@ConditionalOnProperty(
        prefix = "acme.config.client",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = "com.acme.config_client_starter")
@Import({
        KafkaConfigConsumer.class,
        ConfigSyncJob.class
})
public class ConfigClientAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ConfigClientAutoConfiguration.class);

    /**
     * Cache local para configurações
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "acme.config.client.cache",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public LocalConfigCache localConfigCache(ConfigClientProperties properties,
                                             ConversionService conversionService,
                                             ObjectMapper objectMapper) {
        logger.info("Creating LocalConfigCache with properties: {}", properties.getCache());
        return new LocalConfigCache(properties, conversionService, objectMapper);
    }

    /**
     * WebClient para comunicação com a API
     */
    @Bean
    @ConditionalOnMissingBean(name = "configWebClient")
    public WebClient configWebClient(ConfigClientProperties properties) {
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(properties.getServer().getBaseUrl());

        // Adiciona headers customizados se configurados
        if (!properties.getServer().getHeaders().isEmpty()) {
            builder.defaultHeaders(headers ->
                    properties.getServer().getHeaders().forEach(headers::add)
            );
        }

        WebClient webClient = builder.build();

        logger.info("Created WebClient for Config API: {}",
                properties.getServer().getBaseUrl());

        return webClient;
    }
    /**
     * WebClient.Builder para criação de WebClient instances
     */
    @Bean
    @ConditionalOnMissingBean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }


    /**
     * Serviço principal de configurações
     */
    @Bean
    @ConditionalOnMissingBean
    public ConfigService configService(
            LocalConfigCache cache,
            WebClient.Builder webClientBuilder,
            ConfigClientProperties properties,
            ApplicationEventPublisher eventPublisher,
            ConversionService conversionService,
            ObjectMapper objectMapper) {

        logger.info("Creating ConfigService for namespace='{}', environment='{}'",
                properties.getApplication(), properties.getProfile());

        return new ConfigService(
                cache, webClientBuilder, properties,
                eventPublisher, conversionService, objectMapper
        );
    }

    /**
     * Post-processor para injeção de @ConfigValue
     */
    @Bean
    @ConditionalOnMissingBean
    public ConfigValuePostProcessor configValuePostProcessor(
            ConfigService configService,
            ConfigClientProperties properties) {

        logger.info("Creating ConfigValuePostProcessor for @ConfigValue annotation processing");
        return new ConfigValuePostProcessor(configService, properties);
    }

    /**
     * ObjectMapper para serialização JSON (se não existir)
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(ObjectMapper.class)
    public ObjectMapper configObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }

    /**
     * Configuração condicional para Kafka
     */
    @AutoConfiguration
    @ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
    @ConditionalOnProperty(
            prefix = "acme.config.client.kafka",
            name = "enabled",
            havingValue = "true"
    )
    public static class KafkaAutoConfiguration {

        private static final Logger logger = LoggerFactory.getLogger(KafkaAutoConfiguration.class);

        @Bean
        @ConditionalOnMissingBean
        public KafkaConfigConsumer kafkaConfigConsumer(
                ConfigService configService,
                ConfigClientProperties properties,
                ObjectMapper objectMapper) {

            logger.info("Creating KafkaConfigConsumer for topic: {}",
                    properties.getKafka().getTopic());

            return new KafkaConfigConsumer(configService, properties, objectMapper);
        }
    }

    /**
     * Configuração condicional para Quartz
     */
    @AutoConfiguration
    @ConditionalOnClass(name = "org.quartz.Scheduler")
    @ConditionalOnProperty(
            prefix = "acme.config.client.sync",
            name = "enabled",
            havingValue = "true"
    )
    public static class QuartzAutoConfiguration {

        private static final Logger logger = LoggerFactory.getLogger(QuartzAutoConfiguration.class);

        @Bean
        @ConditionalOnMissingBean
        public ConfigSyncJob configSyncJob() {
            logger.info("Creating ConfigSyncJob for periodic synchronization");
            return new ConfigSyncJob();
        }
    }

    /**
     * Configuração condicional para Caffeine Cache
     */
    @AutoConfiguration
    @ConditionalOnClass(name = "com.github.benmanes.caffeine.cache.Caffeine")
    public static class CaffeineAutoConfiguration {

        private static final Logger logger = LoggerFactory.getLogger(CaffeineAutoConfiguration.class);


        public CaffeineAutoConfiguration() {
            logger.debug("Caffeine cache support is available");
        }
    }
}