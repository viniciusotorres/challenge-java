package com.acme.config_client_starter.event;

import org.springframework.context.ApplicationEvent;

import java.time.Instant;

/**
 * Evento disparado quando uma configuração é alterada
 */
public class ConfigChangeEvent extends ApplicationEvent {

    private final String key;
    private final String oldValue;
    private final String newValue;
    private final String operation;
    private final Instant timestamp;

    /**
     * Cria um novo evento de mudança de configuração
     *
     * @param source fonte do evento
     * @param key chave da configuração alterada
     * @param oldValue valor anterior (null para CREATE)
     * @param newValue novo valor (null para DELETE)
     * @param operation tipo de operação: CREATE, UPDATE, DELETE
     */
    public ConfigChangeEvent(Object source, String key, String oldValue, String newValue, String operation) {
        super(source);
        this.key = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.operation = operation;
        this.timestamp = Instant.now();
    }

    /**
     * Construtor padrão para deserialização
     */
    public ConfigChangeEvent() {
        super("");
        this.key = null;
        this.oldValue = null;
        this.newValue = null;
        this.operation = null;
        this.timestamp = Instant.now();
    }

    public String getKey() {
        return key;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public String getOperation() {
        return operation;
    }


    /**
     * Verifica se a operação foi de criação
     */
    public boolean isCreate() {
        return "CREATE".equals(operation);
    }

    /**
     * Verifica se a operação foi de atualização
     */
    public boolean isUpdate() {
        return "UPDATE".equals(operation);
    }

    /**
     * Verifica se a operação foi de remoção
     */
    public boolean isDelete() {
        return "DELETE".equals(operation);
    }

    /**
     * Verifica se o valor da configuração mudou efetivamente
     */
    public boolean hasValueChanged() {
        if (isCreate()) {
            return newValue != null;
        }
        if (isDelete()) {
            return oldValue != null;
        }
        if (isUpdate()) {
            return !java.util.Objects.equals(oldValue, newValue);
        }
        return false;
    }

    @Override
    public String toString() {
        return "ConfigChangeEvent{" +
                "key='" + key + '\'' +
                ", oldValue='" + oldValue + '\'' +
                ", newValue='" + newValue + '\'' +
                ", operation='" + operation + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConfigChangeEvent that = (ConfigChangeEvent) o;

        if (!java.util.Objects.equals(key, that.key)) return false;
        if (!java.util.Objects.equals(oldValue, that.oldValue)) return false;
        if (!java.util.Objects.equals(newValue, that.newValue)) return false;
        if (!java.util.Objects.equals(operation, that.operation)) return false;
        return java.util.Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (oldValue != null ? oldValue.hashCode() : 0);
        result = 31 * result + (newValue != null ? newValue.hashCode() : 0);
        result = 31 * result + (operation != null ? operation.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        return result;
    }
}