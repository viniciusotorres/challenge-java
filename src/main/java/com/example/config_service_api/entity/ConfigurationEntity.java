package com.example.config_service_api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "configurations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @NotNull(message = "NameSpace é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "namespace_id")
    private Namespace namespace;

    @NotNull(message = "Environment é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id")
    private Environment environment;

    @NotBlank(message = "Key não pode estar vazia")
    @Size(max = 255, message = "Key não pode ter mais de 255 caracteres")
    @Column(nullable = false, length = 255)
    private String key;

    @NotBlank(message = "Value não pode estar vazia")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @LastModifiedDate
    private LocalDateTime updatedAt;


}
