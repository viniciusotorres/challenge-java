package com.acme.config_client_starter.annotation;

import java.lang.annotation.*;

/**
 * Annotation para injeção automática de valores de configuração distribuída.
 *
 * Exemplo de uso:
 * <pre>
 * {@code
 * @ConfigValue("database.host")
 * private String dbHost;
 *
 * @ConfigValue(value = "app.timeout", defaultValue = "30")
 * private int timeout;
 *
 * @ConfigValue(value = "feature.enabled", required = false)
 * private Boolean featureEnabled;
 * }
 * </pre>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConfigValue {

    /**
     * Chave da configuração a ser injetada.
     * Suporta notação de ponto (ex: "database.host", "app.feature.enabled")
     */
    String value();

    /**
     * Valor padrão caso a configuração não seja encontrada.
     * Se não especificado e required=true, será lançada exceção.
     */
    String defaultValue() default "";

    /**
     * Indica se a configuração é obrigatória.
     * Se true e a configuração não for encontrada, lança exceção.
     */
    boolean required() default true;

    /**
     * Refresh automático quando a configuração for alterada.
     * Se true, o campo será atualizado automaticamente via reflection.
     */
    boolean autoRefresh() default true;

    /**
     * Tipo de conversão para valores complexos.
     * Por padrão, usa conversão automática baseada no tipo do campo.
     */
    Class<?> type() default Object.class;

    /**
     * Aplicação de origem da configuração.
     * Se não especificado, usa a aplicação atual.
     */
    String application() default "";

    /**
     * Perfil específico da configuração.
     * Se não especificado, usa o perfil ativo atual.
     */
    String profile() default "";
}