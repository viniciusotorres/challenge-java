package com.acme.config_client_starter.processor;

import com.acme.config_client_starter.annotation.ConfigValue;
import com.acme.config_client_starter.configuration.ConfigClientProperties;
import com.acme.config_client_starter.event.ConfigUpdateEvent;
import com.acme.config_client_starter.service.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Post-processor para injeção automática de valores via @ConfigValue.
 * Suporta refresh automático quando configurações são atualizadas.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class ConfigValuePostProcessor implements BeanPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ConfigValuePostProcessor.class);

    private final ConfigService configService;
    private final ConfigClientProperties properties;

    // Mapa para rastrear campos anotados que suportam auto-refresh
    private final Map<String, FieldInfo> annotatedFields = new ConcurrentHashMap<>();

    public ConfigValuePostProcessor(ConfigService configService,
                                    ConfigClientProperties properties) {
        this.configService = configService;
        this.properties = properties;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clazz = bean.getClass();

        ReflectionUtils.doWithFields(clazz, field -> {
            ConfigValue annotation = field.getAnnotation(ConfigValue.class);
            if (annotation != null) {
                processConfigValueField(bean, field, annotation, beanName);
            }
        }, field -> field.isAnnotationPresent(ConfigValue.class));

        return bean;
    }

    /**
     * Processa um field anotado com @ConfigValue
     */
    private void processConfigValueField(Object bean, Field field, ConfigValue annotation, String beanName) {
        try {
            field.setAccessible(true);

            String namespace = resolveApplication(annotation);
            String environment = resolveProfile(annotation);
            String key = annotation.value();

            Object value = resolveConfigValue(namespace, environment, key, field.getType(), annotation);

            if (value != null) {
                field.set(bean, value);

                if (annotation.autoRefresh()) {
                    String fullKey = String.format("%s:%s:%s", namespace, environment, key);
                    annotatedFields.put(fullKey, new FieldInfo(bean, field, annotation, beanName));
                    logger.debug("Registered field for auto-refresh: {}.{} -> {}",
                            beanName, field.getName(), fullKey);
                }

                logger.debug("Injected config value: {} = {} into {}.{}",
                        key, value, beanName, field.getName());
            } else if (annotation.required()) {
                throw new IllegalStateException(
                        String.format("Required configuration key '%s' not found for field %s.%s",
                                key, beanName, field.getName())
                );
            } else {
                logger.debug("Optional config key '{}' not found for field {}.{}, using null",
                        key, beanName, field.getName());
            }

        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to inject config value", e);
        }
    }

    /**
     * Resolve valor da configuração com fallbacks
     */
    private Object resolveConfigValue(String application, String profile, String key,
                                      Class<?> fieldType, ConfigValue annotation) {

        // 1. Tenta buscar valor específico
        Object value = configService.getValue(application, profile, key, fieldType).orElse(null);

        // 2. Se não encontrou e tem default, usa o default
        if (value == null && StringUtils.hasText(annotation.defaultValue())) {
            value = convertDefaultValue(annotation.defaultValue(), fieldType);
        }

        return value;
    }

    /**
     * Converte valor padrão string para tipo específico
     */
    private Object convertDefaultValue(String defaultValue, Class<?> targetType) {
        try {
            return configService.convertValue(defaultValue, targetType);
        } catch (Exception e) {
            logger.warn("Failed to convert default value '{}' to type {}",
                    defaultValue, targetType.getSimpleName(), e);
            return null;
        }
    }

    /**
     * Resolve application da configuração
     */
    private String resolveApplication(ConfigValue annotation) {
        if (StringUtils.hasText(annotation.application())) {
            return annotation.application();
        }
        return properties.getApplication();
    }

    /**
     * Resolve profile da configuração
     */
    private String resolveProfile(ConfigValue annotation) {
        if (StringUtils.hasText(annotation.profile())) {
            return annotation.profile();
        }
        return properties.getProfile();
    }

    /**
     * Escuta eventos de atualização de configuração para fazer auto-refresh
     */
    @EventListener
    public void handleConfigUpdate(ConfigUpdateEvent event) {
        String fullKey = String.format("%s:%s:%s",
                event.getApplication(), event.getProfile(), event.getConfigKey());

        FieldInfo fieldInfo = annotatedFields.get(fullKey);
        if (fieldInfo != null) {
            try {
                Object newValue = configService.convertValue(event.getNewValue(), fieldInfo.field.getType());
                fieldInfo.field.set(fieldInfo.bean, newValue);

                logger.info("Auto-refreshed field {}.{} with new value: {} (was: {})",
                        fieldInfo.beanName, fieldInfo.field.getName(),
                        event.getNewValue(), event.getOldValue());

            } catch (Exception e) {
                logger.error("Failed to auto-refresh field {}.{} for config update: {}",
                        fieldInfo.beanName, fieldInfo.field.getName(), fullKey, e);
            }
        }
    }

    /**
     * Obtém estatísticas dos campos registrados para auto-refresh
     */
    public Map<String, String> getRegisteredFieldsStats() {
        Map<String, String> stats = new ConcurrentHashMap<>();

        annotatedFields.forEach((key, fieldInfo) -> {
            String fieldName = String.format("%s.%s", fieldInfo.beanName, fieldInfo.field.getName());
            stats.put(key, fieldName);
        });

        return stats;
    }

    /**
     * Remove field do registro de auto-refresh (útil para cleanup)
     */
    public void unregisterField(String application, String profile, String key) {
        String fullKey = String.format("%s:%s:%s", application, profile, key);
        FieldInfo removed = annotatedFields.remove(fullKey);
        if (removed != null) {
            logger.debug("Unregistered field for auto-refresh: {}.{}",
                    removed.beanName, removed.field.getName());
        }
    }

    /**
     * Informações sobre um campo anotado
     */
    private static class FieldInfo {
        final Object bean;
        final Field field;
        final ConfigValue annotation;
        final String beanName;

        FieldInfo(Object bean, Field field, ConfigValue annotation, String beanName) {
            this.bean = bean;
            this.field = field;
            this.annotation = annotation;
            this.beanName = beanName;
        }
    }
}