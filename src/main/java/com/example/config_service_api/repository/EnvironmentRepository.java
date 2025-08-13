package com.example.config_service_api.repository;

import com.example.config_service_api.entity.EnvironmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EnvironmentRepository extends JpaRepository<EnvironmentEntity, UUID> {
}
