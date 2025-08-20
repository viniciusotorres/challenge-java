package com.example.config_service_api.enums;

public enum OperationType {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete");

   private final String description;

    OperationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}
