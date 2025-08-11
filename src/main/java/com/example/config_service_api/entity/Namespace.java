package com.example.config_service_api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

@Entity
@Table(name = "namespaces")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Namespace {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Nome do Namespace é obrigatório")
    private String name;
}
