package com.acme.config_client_starter.sync;

import com.acme.config_client_starter.cache.LocalConfigCache;
import com.acme.config_client_starter.configuration.ConfigClientProperties;
import com.acme.config_client_starter.service.ConfigService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Job Quartz para sincronização periódica de configurações.
 * Executa em intervalos configurados para manter cache atualizado.
 */
@Component
@ConditionalOnProperty(
        prefix = "acme.config.client.sync",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ConfigSyncJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(ConfigSyncJob.class);
    private static final String JOB_NAME = "configSyncJob";
    private static final String TRIGGER_NAME = "configSyncTrigger";

    @Autowired
    private ConfigService configService;

    @Autowired
    private ConfigClientProperties properties;

    @Autowired
    private LocalConfigCache cache;

    private final AtomicLong syncCount = new AtomicLong(0);
    private volatile Instant lastSyncTime;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        long syncNumber = syncCount.incrementAndGet();
        Instant startTime = Instant.now();

        logger.debug("Starting periodic config sync #{}", syncNumber);

        try {
            performSync();
            lastSyncTime = Instant.now();

            long durationMs = java.time.Duration.between(startTime, lastSyncTime).toMillis();
            logger.info("Config sync #{} completed in {}ms", syncNumber, durationMs);

        } catch (Exception e) {
            logger.error("Config sync #{} failed", syncNumber, e);
            throw new JobExecutionException("Failed to sync configurations", e);
        }
    }

    /**
     * Executa sincronização das configurações
     */
    private void performSync() {
        String namespace = properties.getApplication();
        String environment = properties.getProfile();

        if (properties.getSync().getKeys().isEmpty()) {
            syncAllConfigurations(namespace, environment);
        } else {
            syncSpecificKeys(namespace, environment, properties.getSync().getKeys());
        }
    }

    /**
     * Sincroniza todas as configurações do environment
     */
    private void syncAllConfigurations(String namespace, String environment) {
        CompletableFuture<java.util.Map<String, Object>> future =
                configService.getAllConfigurations(namespace, environment);

        future.whenComplete((configs, throwable) -> {
            if (throwable != null) {
                logger.warn("Failed to sync all configurations for {}:{}",
                        namespace, environment, throwable);
            } else {
                logger.debug("Synced {} configurations for {}:{}",
                        configs.size(), namespace, environment);
            }
        });
    }

    /**
     * Sincroniza apenas chaves específicas
     */
    private void syncSpecificKeys(String namespace, String environment, java.util.List<String> keys) {
        java.util.List<CompletableFuture<Void>> futures = keys.stream()
                .map(key -> syncSingleKey(namespace, environment, key))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        logger.warn("Some key syncs failed for {}:{}", namespace, environment, throwable);
                    } else {
                        logger.debug("Synced {} specific keys for {}:{}",
                                keys.size(), namespace, environment);
                    }
                });
    }

    /**
     * Sincroniza uma única chave
     */
    private CompletableFuture<Void> syncSingleKey(String namespace, String environment, String key) {
        return configService.refreshConfiguration(namespace, environment, key)
                .thenAccept(value -> {
                    if (value.isPresent()) {
                        logger.trace("Refreshed config: {}:{}:{} = {}",
                                namespace, environment, key, value.get());
                    } else {
                        logger.trace("Config not found during sync: {}:{}:{}",
                                namespace, environment, key);
                    }
                })
                .exceptionally(throwable -> {
                    logger.warn("Failed to sync config {}:{}:{}",
                            namespace, environment, key, throwable);
                    return null;
                });
    }

    /**
     * Retorna estatísticas do sync
     */
    public SyncStats getSyncStats() {
        return new SyncStats(
                syncCount.get(),
                lastSyncTime,
                cache.getStats()
        );
    }

    /**
     * Configuração Quartz para o job
     */
    @Configuration
    @ConditionalOnProperty(
            prefix = "acme.config.client.sync",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public static class QuartzConfig {

        @Bean
        public JobDetailFactoryBean configSyncJobDetail() {
            JobDetailFactoryBean factory = new JobDetailFactoryBean();
            factory.setJobClass(ConfigSyncJob.class);
            factory.setName(JOB_NAME);
            factory.setDescription("Periodic configuration synchronization job");
            factory.setDurability(true);
            return factory;
        }

        @Bean
        public CronTriggerFactoryBean configSyncTrigger(ConfigClientProperties properties) {
            CronTriggerFactoryBean factory = new CronTriggerFactoryBean();
            factory.setJobDetail(configSyncJobDetail().getObject());
            factory.setName(TRIGGER_NAME);
            factory.setDescription("Trigger for periodic configuration synchronization");
            factory.setCronExpression(properties.getSync().getCron());
            return factory;
        }

        @Bean
        public SchedulerFactoryBean configSyncScheduler(ConfigClientProperties properties) {
            SchedulerFactoryBean factory = new SchedulerFactoryBean();
            factory.setTriggers(configSyncTrigger(properties).getObject());
            factory.setAutoStartup(properties.getSync().isEnabled());
            factory.setStartupDelay(properties.getSync().isOnStartup() ? 0 : 60);

            java.util.Properties quartzProps = new java.util.Properties();
            quartzProps.setProperty("org.quartz.scheduler.instanceName", "ConfigSyncScheduler");
            quartzProps.setProperty("org.quartz.scheduler.instanceId", "AUTO");
            quartzProps.setProperty("org.quartz.threadPool.threadCount", "2");
            quartzProps.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
            quartzProps.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");

            factory.setQuartzProperties(quartzProps);
            return factory;
        }
    }

    /**
     * Estatísticas de sincronização
     */
    public static class SyncStats {
        private final long syncCount;
        private final Instant lastSyncTime;
        private final LocalConfigCache.CacheStats cacheStats;

        public SyncStats(long syncCount, Instant lastSyncTime, LocalConfigCache.CacheStats cacheStats) {
            this.syncCount = syncCount;
            this.lastSyncTime = lastSyncTime;
            this.cacheStats = cacheStats;
        }

        public long getSyncCount() { return syncCount; }
        public Instant getLastSyncTime() { return lastSyncTime; }
        public LocalConfigCache.CacheStats getCacheStats() { return cacheStats; }

        @Override
        public String toString() {
            return String.format(
                    "SyncStats{syncCount=%d, lastSync=%s, cache=%s}",
                    syncCount, lastSyncTime, cacheStats
            );
        }
    }
}