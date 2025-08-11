package com.example.config_service_api.repository;

import com.example.config_service_api.entity.ConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConfigurationRepository extends JpaRepository<ConfigurationEntity, UUID> {
    Optional<ConfigurationEntity> findByKeyAndValue(String key, String value);
    boolean existsByKeyAndValue(String key, String value);

}
