package com.example.config_service_api.repository;

import com.example.config_service_api.dto.HistoryConfigResponseDto;
import com.example.config_service_api.entity.HistoryConfigEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HistoryConfigRepository extends JpaRepository<HistoryConfigEntity, UUID> {
    Page<HistoryConfigEntity> findByEnvironmentId(UUID environmentId, Pageable pageable);
}
