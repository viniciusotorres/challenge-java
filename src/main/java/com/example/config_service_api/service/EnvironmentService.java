package com.example.config_service_api.service;

import com.example.config_service_api.dto.*;
import com.example.config_service_api.entity.EnvironmentEntity;
import com.example.config_service_api.entity.NamespaceEntity;
import com.example.config_service_api.repository.EnvironmentRepository;
import com.example.config_service_api.repository.NamespaceRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EnvironmentService {

    private Logger logger = LoggerFactory.getLogger(EnvironmentService.class);

    private final EnvironmentRepository environmentRepository;
    private final NamespaceRepository namespaceRepository;

    public EnvironmentService(EnvironmentRepository environmentRepository, NamespaceRepository namespaceRepository) {
        this.environmentRepository = environmentRepository;
        this.namespaceRepository = namespaceRepository;
    }

    public ResponseDto<EnvironmentResponseDto> createEnvironment(EnvironmentCreateDto dto) {
        logger.info("Criando ambiente - Nome: {}", dto.name());

        logger.info("Buscando namespace - ID: {}", dto.namespaceId());
        Optional<NamespaceEntity> namespaceOpt = namespaceRepository.findById(dto.namespaceId());

        if (namespaceOpt.isEmpty()) {
            logger.warn("Namespace não encontrado - ID: {}", dto.namespaceId());
            throw new EntityNotFoundException("Namespace não encontrado");
        }


        logger.info("Namespace encontrado - ID: {}, Nome: {}", namespaceOpt.get().getId(), namespaceOpt.get().getName());

        if (environmentRepository.existsByName(dto.name())) {
            logger.warn("Ambiente já existe - Nome: {}", dto.name());
            throw new DataIntegrityViolationException("Ambiente com nome já existente");
        }

        EnvironmentEntity environment = buildEnvironment(dto, namespaceOpt);

        saveEnvironment(environment);

        EnvironmentResponseDto responseDto = toResponseDto(environment);

        logger.info("Ambiente criado com sucesso - ID: {}, Nome: {}", environment.getId(), environment.getName());

        return ResponseDto.<EnvironmentResponseDto>builder()
                .data(responseDto)
                .message("Ambiente criado com sucesso")
                .success(true)
                .statusCode(201)
                .build();
    }

    private EnvironmentEntity buildEnvironment(EnvironmentCreateDto dto, Optional<NamespaceEntity> namespaceOpt) {
        return EnvironmentEntity.builder()
                .name(dto.name())
                .namespace(namespaceOpt.get())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private EnvironmentResponseDto toResponseDto(EnvironmentEntity env) {
        return new EnvironmentResponseDto(
                env.getId(),
                env.getName(),
                env.getCreatedAt(),
                env.getUpdatedAt(),
                new NamespaceResponseDto(
                        env.getNamespace().getId(),
                        env.getNamespace().getName(),
                        env.getNamespace().getCreatedAt(),
                        env.getNamespace().getUpdatedAt()
                )
        );
    }

    private void saveEnvironment(EnvironmentEntity environment) {
        logger.info("Persistindo ambiente no banco - Nome: {}", environment.getName());
        environmentRepository.save(environment);
    }

    public ResponseDto<PageableDto<EnvironmentResponseDto>> listEnvironments(Pageable pageable) {
        logger.info("Listando ambientes com paginação - Página: {}, Tamanho: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        var environments = environmentRepository.findAll(pageable);

        List<EnvironmentResponseDto> contentDto = environments.getContent()
                .stream()
                .map(this::toResponseDto)
                .toList();

       PageableDto<EnvironmentResponseDto> pageableDto = PageableDto.<EnvironmentResponseDto>builder()
                .content(contentDto)
                .currentPage(environments.getNumber())
                .pageSize(environments.getSize())
                .totalElements((int) environments.getTotalElements())
                .totalPages(environments.getTotalPages())
               .first(environments.isFirst())
                .build();

        logger.info("Ambientes encontrados - Total: {}", pageableDto.totalElements());

        return ResponseDto.<PageableDto<EnvironmentResponseDto>>builder()
                .data(pageableDto)
                .message("Ambientes listados com sucesso")
                .success(true)
                .statusCode(200)
                .build();
    }

    public ResponseDto<EnvironmentResponseDto> updateEnvironment(UUID id, EnvironmentUpdateDto dto) {
        logger.info("Atualizando ambiente - ID: {}, Nome: {}", id, dto.name());

        return environmentRepository.findById(id)
                .map(environment -> {
                    if (!environment.getName().equals(dto.name()) && environmentRepository.existsByName(dto.name())) {
                        logger.warn("Ambiente com nome já existente - Nome: {}", dto.name());
                        throw new DataIntegrityViolationException("Ambiente com nome já existente");
                    }

                    environment.setName(dto.name());
                    environment.setUpdatedAt(LocalDateTime.now());
                    saveEnvironment(environment);

                    EnvironmentResponseDto responseDto = toResponseDto(environment);

                    logger.info("Ambiente atualizado com sucesso - ID: {}, Nome: {}", environment.getId(), environment.getName());

                    return ResponseDto.<EnvironmentResponseDto>builder()
                            .data(responseDto)
                            .message("Ambiente atualizado com sucesso")
                            .success(true)
                            .statusCode(200)
                            .build();
                })
                .orElseGet(() -> {
                    logger.warn("Ambiente não encontrado para atualização - ID: {}", id);
                    throw new EntityNotFoundException("Ambiente não encontrado");
                });
    }

    public ResponseDto<EnvironmentResponseDto> getEnvironmentById(UUID id) {
        logger.info("Buscando ambiente por ID - ID: {}", id);

        return environmentRepository.findById(id)
                .map(environment -> {
                    EnvironmentResponseDto responseDto = toResponseDto(environment);
                    logger.info("Ambiente encontrado - ID: {}, Nome: {}", environment.getId(), environment.getName());
                    return ResponseDto.<EnvironmentResponseDto>builder()
                            .data(responseDto)
                            .message("Ambiente encontrado")
                            .success(true)
                            .statusCode(200)
                            .build();
                })
                .orElseGet(() -> {
                    logger.warn("Ambiente não encontrado - ID: {}", id);
                    throw new EntityNotFoundException("Ambiente não encontrado");
                });
    }

    public ResponseDto<Void> deleteEnvironment(UUID id) {
        logger.info("Deletando ambiente - ID: {}", id);

        return environmentRepository.findById(id)
                .map(environment -> {
                    environmentRepository.delete(environment);
                    logger.info("Ambiente deletado com sucesso - ID: {}", id);
                    return ResponseDto.<Void>builder()
                            .message("Ambiente deletado com sucesso")
                            .success(true)
                            .statusCode(204)
                            .build();
                })
                .orElseGet(() -> {
                    logger.warn("Ambiente não encontrado para deleção - ID: {}", id);
                    throw new EntityNotFoundException("Ambiente não encontrado");
                });
    }
}
