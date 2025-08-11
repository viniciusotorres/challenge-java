package com.example.config_service_api.repository;

import com.example.config_service_api.entity.Environment;
import com.example.config_service_api.entity.Namespace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NamespaceRepository extends JpaRepository <Namespace, UUID> {
    Optional<Namespace> findByName(String name);
}
