package com.example.config_service_api.service;

import com.example.config_service_api.dto.*;
import com.example.config_service_api.entity.EnvironmentEntity;
import com.example.config_service_api.entity.NamespaceEntity;
import com.example.config_service_api.repository.EnvironmentRepository;
import com.example.config_service_api.repository.NamespaceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NamespaceService {

    private Logger logger = LoggerFactory.getLogger(NamespaceService.class);

    private final NamespaceRepository namespaceRepository;
    private final EnvironmentRepository environmentRepository;

    public NamespaceService(NamespaceRepository namespaceRepository, EnvironmentRepository environmentRepository) {
        this.namespaceRepository = namespaceRepository;
        this.environmentRepository = environmentRepository;
    }

    public ResponseDto<NamespaceEntity> createNamespace(NamespaceCreateDto dto) {
        logger.info("Criando namespace - Nome: {}", dto.name());

        if (namespaceRepository.existsByName(dto.name())) {
            logger.warn("Namespace já existe - Nome: {}", dto.name());
            throw new DataIntegrityViolationException("Namespace com nome já existente");
        }

        NamespaceEntity namespace = buildNamespace(dto);

        saveNamespace(namespace);

        logger.info("Namespace criado com sucesso - ID: {}, Nome: {}", namespace.getId(), namespace.getName());

        return ResponseDto.<NamespaceEntity>builder()
                .data(namespace)
                .message("Namespace criado com sucesso")
                .success(true)
                .statusCode(201)
                .build();
    }

    private NamespaceEntity buildNamespace(NamespaceCreateDto dto) {
        return NamespaceEntity.builder()
                .name(dto.name())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private void saveNamespace(NamespaceEntity namespace) {
        logger.info("Persistindo namespace no banco - Nome: {}", namespace.getName());
        namespaceRepository.save(namespace);
    }

    public ResponseDto<PageableDto<NamespaceResponseDto>> listNamespaces(Pageable pageable) {
        logger.info("Listando namespace com paginação - Página: {}, Tamanho: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        var namespacesPage = namespaceRepository.findAll(pageable);

        List<NamespaceResponseDto> contentDto = namespacesPage.getContent()
                .stream()
                .map(this::toResponseDto)
                .toList();

        PageableDto<NamespaceResponseDto> pageableDto = PageableDto.<NamespaceResponseDto>builder()
                .content(contentDto)
                .currentPage(namespacesPage.getNumber())
                .pageSize(namespacesPage.getSize())
                .totalElements(namespacesPage.getTotalElements())
                .totalPages(namespacesPage.getTotalPages())
                .first(namespacesPage.isFirst())
                .build();

        logger.info("Namespaces encontrados - Total: {}", pageableDto.totalElements());

        return ResponseDto.<PageableDto<NamespaceResponseDto>>builder()
                .data(pageableDto)
                .message("Namespaces listados com sucesso")
                .success(true)
                .statusCode(200)
                .build();
    }

    private NamespaceResponseDto toResponseDto(NamespaceEntity namespace) {
        return new NamespaceResponseDto(
                namespace.getId(),
                namespace.getName(),
                namespace.getCreatedAt(),
                namespace.getUpdatedAt()
        );
    }

    public ResponseDto<NamespaceResponseDto> updateNamespace(UUID id, NamespaceUpdateDto dto) {
        logger.info("Atualizando namespace - ID: {}, Nome: {}", id, dto.name());

        return namespaceRepository.findById(id)
                .map(namespace -> {
                    if (!namespace.getName().equals(dto.name()) && namespaceRepository.existsByName(dto.name())) {
                        logger.warn("Namespace já existe com o nome - Nome: {}", dto.name());
                        throw new DataIntegrityViolationException("Namespace com nome já existente");
                    }

                    namespace.setName(dto.name());
                    namespace.setUpdatedAt(LocalDateTime.now());
                    saveNamespace(namespace);

                    logger.info("Namespace atualizado com sucesso - ID: {}, Nome: {}", namespace.getId(), namespace.getName());

                    NamespaceResponseDto responseDto = new NamespaceResponseDto(
                            namespace.getId(),
                            namespace.getName(),
                            namespace.getCreatedAt(),
                            namespace.getUpdatedAt()
                    );

                    return ResponseDto.<NamespaceResponseDto>builder()
                            .data(responseDto)
                            .message("Namespace atualizado com sucesso")
                            .success(true)
                            .statusCode(200)
                            .build();
                })
                .orElseGet(() -> {
                    logger.warn("Namespace não encontrado para atualização - ID: {}", id);
                    throw new EntityNotFoundException("Namespace não encontrado");
                });
    }

    public ResponseDto<NamespaceResponseDto> getNamespaceById(UUID id) {
        logger.info("Buscando namespace por ID - ID: {}", id);

        return namespaceRepository.findById(id)
                .map(namespace -> {
                    logger.info("Namespace encontrado - ID: {}, Nome: {}", namespace.getId(), namespace.getName());

                    NamespaceResponseDto dto = new NamespaceResponseDto(
                            namespace.getId(),
                            namespace.getName(),
                            namespace.getCreatedAt(),
                            namespace.getUpdatedAt()
                    );

                    return ResponseDto.<NamespaceResponseDto>builder()
                            .data(dto)
                            .message("Namespace encontrado")
                            .success(true)
                            .statusCode(200)
                            .build();
                })
                .orElseGet(() -> {
                    logger.warn("Namespace não encontrado - ID: {}", id);
                    throw new EntityNotFoundException("Namespace não encontrado");
                });
    }

    public ResponseDto<Void> deleteNamespace(UUID id) {
        logger.info("Deletando namespace - ID: {}", id);

        return namespaceRepository.findById(id)
                .map(namespace -> {
                    namespaceRepository.delete(namespace);
                    logger.info("Namespace deletado com sucesso - ID: {}", id);
                    return ResponseDto.<Void>builder()
                            .message("Namespace deletado com sucesso")
                            .success(true)
                            .statusCode(204)
                            .build();
                })
                .orElseGet(() -> {
                    logger.warn("Namespace não encontrado para deleção - ID: {}", id);
                    throw new EntityNotFoundException("Namespace não encontrado");
                });
    }

    public ResponseDto<PageableDto<EnvironmentSimpleResponseDto>> getEnvironmentsByNamespaceId(UUID id, Pageable pageable) {
        logger.info("Buscando ambientes por namespace - Namespace ID: {}, Página: {}, Tamanho: {}",
                id, pageable.getPageNumber(), pageable.getPageSize());

        if (!namespaceRepository.existsById(id)) {
            logger.warn("Namespace não encontrado - ID: {}", id);
            throw new EntityNotFoundException("Namespace não encontrado");
        }

        var environmentsPage = environmentRepository.findByNamespaceId(id, pageable);

        List<EnvironmentSimpleResponseDto> contentDto = environmentsPage.getContent()
                .stream()
                .map(env -> new EnvironmentSimpleResponseDto(
                        env.getId(),
                        env.getName(),
                        env.getCreatedAt(),
                        env.getUpdatedAt()
                ))
                .toList();

        PageableDto<EnvironmentSimpleResponseDto> pageableDto = PageableDto.<EnvironmentSimpleResponseDto>builder()
                .content(contentDto)
                .currentPage(environmentsPage.getNumber())
                .pageSize(environmentsPage.getSize())
                .totalElements(environmentsPage.getTotalElements())
                .totalPages(environmentsPage.getTotalPages())
                .first(environmentsPage.isFirst())
                .build();

        String message = contentDto.isEmpty()
                ? "Nenhum ambiente encontrado vinculado a esse namespace"
                : "Ambientes listados com sucesso";

        logger.info("{} - Namespace ID: {}, Total: {}", message, id, pageableDto.totalElements());

        return ResponseDto.<PageableDto<EnvironmentSimpleResponseDto>>builder()
                .data(pageableDto)
                .message(message)
                .success(true)
                .statusCode(200)
                .build();
    }




}



