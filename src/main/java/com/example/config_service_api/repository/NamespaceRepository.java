package com.example.config_service_api.repository;

import com.example.config_service_api.entity.NamespaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NamespaceRepository extends JpaRepository <NamespaceEntity, UUID> {
    Optional<NamespaceEntity> findByName(String name);

    boolean existsByName(String name);
}
