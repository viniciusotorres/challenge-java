package com.example.config_service_api.dto;

import com.example.config_service_api.enums.OperationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record HistoryConfigResponseDto(
        UUID id,
        UUID environmentId,
        String key,
        String value,
        String changedBy,
        OperationType operationType,
        LocalDateTime changedAt
) {
    public static HistoryConfigResponseDto fromEntity(
            UUID environmentId,
            String oldKey,
            String newKey,
            String oldValue,
            String newValue,
            String changedBy,
            OperationType operationType,
            LocalDateTime changedAt,
            UUID id
    ) {
        String keyToShow;
        String valueToShow;

        switch (operationType) {
            case CREATE -> {
                keyToShow = newKey;
                valueToShow = newValue;
            }
            case DELETE -> {
                keyToShow = oldKey;
                valueToShow = oldValue;
            }
            default -> {
                keyToShow = newKey != null ? newKey : oldKey;
                valueToShow = newValue != null ? newValue : oldValue;
            }
        }

        return new HistoryConfigResponseDto(id, environmentId, keyToShow, valueToShow, changedBy, operationType, changedAt);
    }
}
