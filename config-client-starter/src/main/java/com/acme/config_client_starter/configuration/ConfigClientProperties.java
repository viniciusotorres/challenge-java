package com.acme.config_client_starter.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Propriedades de configuração do client de configuração distribuída.
 */
@ConfigurationProperties(prefix = "acme.config.client")
@Validated
public class ConfigClientProperties {

    /**
     * Habilita o cliente de configuração distribuída
     */
    private boolean enabled = true;

    /**
     * Nome da aplicação para buscar configurações
     */
    @NotBlank
    private String application;

    /**
     * Perfil ativo da aplicação
     */
    private String profile = "default";

    /**
     * Configurações do servidor de configuração
     */
    @NotNull
    private Server server = new Server();

    /**
     * Configurações do Kafka
     */
    @NotNull
    private Kafka kafka = new Kafka();

    /**
     * Configurações do cache local
     */
    @NotNull
    private Cache cache = new Cache();

    /**
     * Configurações de sincronização periódica
     */
    @NotNull
    private Sync sync = new Sync();

    /**
     * Configurações de retry e timeout
     */
    @NotNull
    private Resilience resilience = new Resilience();


    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getApplication() { return application; }
    public void setApplication(String application) { this.application = application; }

    public String getProfile() { return profile; }
    public void setProfile(String profile) { this.profile = profile; }

    public Server getServer() { return server; }
    public void setServer(Server server) { this.server = server; }

    public Kafka getKafka() { return kafka; }
    public void setKafka(Kafka kafka) { this.kafka = kafka; }

    public Cache getCache() { return cache; }
    public void setCache(Cache cache) { this.cache = cache; }

    public Sync getSync() { return sync; }
    public void setSync(Sync sync) { this.sync = sync; }

    public Resilience getResilience() { return resilience; }
    public void setResilience(Resilience resilience) { this.resilience = resilience; }

    /**
     * Configurações do servidor de configuração
     */
    public static class Server {
        /**
         * URL base do servidor de configuração
         */
        @NotBlank
        private String baseUrl = "http://localhost:8080";

        /**
         * Timeout para chamadas HTTP
         */
        private Duration timeout = Duration.ofSeconds(10);

        /**
         * Headers HTTP customizados
         */
        private Map<String, String> headers = Map.of();

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

        public Duration getTimeout() { return timeout; }
        public void setTimeout(Duration timeout) { this.timeout = timeout; }

        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    }

    /**
     * Configurações do Kafka
     */
    public static class Kafka {
        /**
         * Habilita consumer Kafka para updates em tempo real
         */
        private boolean enabled = true;

        /**
         * Bootstrap servers do Kafka
         */
        @NotBlank
        private String bootstrapServers = "localhost:9092";

        /**
         * Tópico para updates de configuração
         */
        @NotBlank
        private String topic = "config-changes";

        /**
         * Group ID do consumer
         */
        @NotBlank
        private String groupId = "config-client";

        /**
         * Configurações adicionais do consumer
         */
        private Map<String, Object> consumerProperties = Map.of();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getBootstrapServers() { return bootstrapServers; }
        public void setBootstrapServers(String bootstrapServers) { this.bootstrapServers = bootstrapServers; }

        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }

        public String getGroupId() { return groupId; }
        public void setGroupId(String groupId) { this.groupId = groupId; }

        public Map<String, Object> getConsumerProperties() { return consumerProperties; }
        public void setConsumerProperties(Map<String, Object> consumerProperties) {
            this.consumerProperties = consumerProperties;
        }
    }

    /**
     * Configurações do cache local
     */
    public static class Cache {
        /**
         * Habilita cache local
         */
        private boolean enabled = true;

        /**
         * Tamanho máximo do cache
         */
        @Positive
        private long maxSize = 1000;

        /**
         * TTL das entradas do cache
         */
        private Duration ttl = Duration.ofHours(1);

        /**
         * Tempo para refresh das entradas
         */
        private Duration refreshAfterWrite = Duration.ofMinutes(30);

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public long getMaxSize() { return maxSize; }
        public void setMaxSize(long maxSize) { this.maxSize = maxSize; }

        public Duration getTtl() { return ttl; }
        public void setTtl(Duration ttl) { this.ttl = ttl; }

        public Duration getRefreshAfterWrite() { return refreshAfterWrite; }
        public void setRefreshAfterWrite(Duration refreshAfterWrite) {
            this.refreshAfterWrite = refreshAfterWrite;
        }
    }

    /**
     * Configurações de sincronização periódica
     */
    public static class Sync {
        /**
         * Habilita sincronização periódica
         */
        private boolean enabled = true;

        /**
         * Intervalo de sincronização (cron expression)
         */
        @NotBlank
        private String cron = "0 */5 * * * ?"; // A cada 5 minutos

        /**
         * Sincronização na inicialização
         */
        private boolean onStartup = true;

        /**
         * Lista de configurações específicas para sincronizar
         */
        private List<String> keys = List.of();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getCron() { return cron; }
        public void setCron(String cron) { this.cron = cron; }

        public boolean isOnStartup() { return onStartup; }
        public void setOnStartup(boolean onStartup) { this.onStartup = onStartup; }

        public List<String> getKeys() { return keys; }
        public void setKeys(List<String> keys) { this.keys = keys; }
    }

    /**
     * Configurações de resiliência
     */
    public static class Resilience {
        /**
         * Número máximo de tentativas
         */
        @Positive
        private int maxRetries = 3;

        /**
         * Delay inicial para retry
         */
        private Duration retryDelay = Duration.ofSeconds(1);

        /**
         * Multiplicador para backoff exponencial
         */
        private double backoffMultiplier = 2.0;

        /**
         * Timeout para operações
         */
        private Duration timeout = Duration.ofSeconds(30);

        /**
         * Fallback para valores padrão quando configuração não encontrada
         */
        private boolean fallbackEnabled = true;

        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

        public Duration getRetryDelay() { return retryDelay; }
        public void setRetryDelay(Duration retryDelay) { this.retryDelay = retryDelay; }

        public double getBackoffMultiplier() { return backoffMultiplier; }
        public void setBackoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
        }

        public Duration getTimeout() { return timeout; }
        public void setTimeout(Duration timeout) { this.timeout = timeout; }

        public boolean isFallbackEnabled() { return fallbackEnabled; }
        public void setFallbackEnabled(boolean fallbackEnabled) {
            this.fallbackEnabled = fallbackEnabled;
        }
    }
}