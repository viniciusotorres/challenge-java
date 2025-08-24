package com.example.config_service_api.repository;

import com.example.config_service_api.entity.DataEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DataRepository extends JpaRepository<DataEntity, UUID> {
    boolean existsByKeyAndEnvironmentId(String key, UUID environmentId);

    Page<DataEntity> findByEnvironmentNamespaceNameAndEnvironmentName(
            String namespaceName,
            String environmentName,
            Pageable pageable);

    Optional<DataEntity> findByKeyAndEnvironmentNamespaceNameAndEnvironmentName(String namespaceName, String environmentName, String key);
}
