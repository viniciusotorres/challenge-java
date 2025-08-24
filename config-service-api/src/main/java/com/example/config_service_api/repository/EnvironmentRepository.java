package com.example.config_service_api.repository;

import com.example.config_service_api.entity.EnvironmentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;
import java.util.UUID;

public interface EnvironmentRepository extends JpaRepository<EnvironmentEntity, UUID> {
    boolean existsByName(String name);
    Page<EnvironmentEntity> findByNamespaceId(UUID namespaceId, Pageable pageable);
    boolean existsByNamespaceId(UUID namespaceId);
    Optional<EnvironmentEntity> findByNamespaceNameAndName(String namespaceName, String environmentName);
}
