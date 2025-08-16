package com.example.config_service_api.entity;

import com.example.config_service_api.enums.OperationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "config_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryConfigEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    private UUID  id;

    @Column(name = "configuration_id")
    private UUID configurationId;

    @Column(name = "environment_id", nullable = false)
    private UUID environmentId;

    private String oldKey;
    private String newKey;

    private String oldValue;
    private String newValue;

    private String changedBy;

    private LocalDateTime changedAt;

    @Enumerated(EnumType.STRING)
    private OperationType operationType;
}
