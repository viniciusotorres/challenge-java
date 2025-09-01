package com.acme.config_client_starter.event;

import org.springframework.context.ApplicationEvent;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Evento publicado quando configurações são atualizadas.
 * Permite que componentes reajam a mudanças de configuração.
 */
public class ConfigUpdateEvent extends ApplicationEvent {

    private final String application;
    private final String profile;
    private final String configKey;
    private final Object oldValue;
    private final Object newValue;
    private final Instant timestamp;
    private final Map<String, Object> metadata;

    public ConfigUpdateEvent(Object source,
                             String application,
                             String profile,
                             String configKey,
                             Object oldValue,
                             Object newValue) {
        this(source, application, profile, configKey, oldValue, newValue, Map.of());
    }

    public ConfigUpdateEvent(Object source,
                             String application,
                             String profile,
                             String configKey,
                             Object oldValue,
                             Object newValue,
                             Map<String, Object> metadata) {
        super(source);
        this.application = application;
        this.profile = profile;
        this.configKey = configKey;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.timestamp = Instant.now();
        this.metadata = Map.copyOf(metadata);
    }

    public String getApplication() {
        return application;
    }

    public String getProfile() {
        return profile;
    }

    public String getConfigKey() {
        return configKey;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }


    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public boolean hasValueChanged() {
        return !Objects.equals(oldValue, newValue);
    }

    public String getFullConfigPath() {
        return String.format("%s:%s:%s", application, profile, configKey);
    }

    @Override
    public String toString() {
        return String.format(
                "ConfigUpdateEvent{app='%s', profile='%s', key='%s', oldValue=%s, newValue=%s, timestamp=%s}",
                application, profile, configKey, oldValue, newValue, timestamp
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConfigUpdateEvent)) return false;
        ConfigUpdateEvent that = (ConfigUpdateEvent) o;
        return Objects.equals(application, that.application) &&
                Objects.equals(profile, that.profile) &&
                Objects.equals(configKey, that.configKey) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(application, profile, configKey, timestamp);
    }
}