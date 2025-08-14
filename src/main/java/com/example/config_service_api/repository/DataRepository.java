package com.example.config_service_api.repository;

import com.example.config_service_api.entity.DataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DataRepository extends JpaRepository<DataEntity, UUID> {
}
