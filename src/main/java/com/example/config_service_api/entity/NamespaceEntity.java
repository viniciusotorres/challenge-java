package com.example.config_service_api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "namespaces", uniqueConstraints = {
        @UniqueConstraint(columnNames = "name")
})

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NamespaceEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, unique = true)
    private String name;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "namespace", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EnvironmentEntity> environments = new ArrayList<>();
}
