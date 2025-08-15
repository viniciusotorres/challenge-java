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
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public ResponseDto<EnvironmentResponseDto> createEnvironment(EnvironmentCreateDto dto) {
        String serviceName = "EnvironmentService";
        String operation = "CREATE_ENVIRONMENT";
        UUID namespaceId = dto.namespaceId();
        String environmentName = dto.name();

        logger.info("[{}] [{}] Iniciando criação de ambiente. Nome: {}, Namespace: {}", serviceName, operation, environmentName, namespaceId);

        logger.debug("[{}] [{}] Buscando namespace no repositório. Namespace ID: {}", serviceName, operation, namespaceId);
        NamespaceEntity namespace = namespaceRepository.findById(dto.namespaceId())
                .orElseThrow(() -> new EntityNotFoundException("Namespace não encontrado"));

        logger.info("[{}] [{}] Namespace encontrado. ID: {}, Nome: {}", serviceName, operation, namespace.getId(), namespace.getName());

        if (environmentRepository.existsByName(environmentName)) {
            logger.warn("[{}] [{}] Ambiente duplicado detectado. Nome: {}", serviceName, operation, environmentName);
            throw new DataIntegrityViolationException(
                    String.format("Ambiente com nome '%s' já existe no namespace %s", environmentName, namespaceId)
            );
        }

        EnvironmentEntity environment = buildEnvironment(dto, namespace);

        logger.debug("[{}] [{}] Persistindo ambiente no banco. Nome: {}, Namespace: {}", serviceName, operation, environmentName, namespaceId);

        environmentRepository.save(environment);

        EnvironmentResponseDto responseDto = toResponseDto(environment);

        logger.info("[{}] [{}] Ambiente criado com sucesso. ID: {}, Nome: {}, Namespace: {}", serviceName, operation, environment.getId(), environmentName, namespaceId);

        return ResponseDto.<EnvironmentResponseDto>builder()
                .data(responseDto)
                .message(String.format("Ambiente '%s' criado com sucesso no namespace %s", environmentName, namespaceId))
                .success(true)
                .statusCode(201)
                .build();
    }

    private EnvironmentEntity buildEnvironment(EnvironmentCreateDto dto, NamespaceEntity namespace) {
        logger.debug("Construindo entidade de ambiente - Nome: {}, Namespace ID: {}", dto.name(), namespace.getId());
        return EnvironmentEntity.builder()
                .name(dto.name())
                .namespace(namespace)
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

    @Transactional(readOnly = true)
    public ResponseDto<PageableDto<EnvironmentResponseDto>> listEnvironments(Pageable pageable) {
        String serviceName = "EnvironmentService";
        String operation = "LIST_ENVIRONMENTS";

        logger.info("[{}] [{}] Iniciando listagem de environments com paginação. Página: {}, Tamanho: {}", serviceName, operation, pageable.getPageNumber(), pageable.getPageSize());

        Page<EnvironmentResponseDto> dtoPage = environmentRepository.findAll(pageable)
                .map(this::toResponseDto);

        PageableDto<EnvironmentResponseDto> pageableDto = PageableDto.<EnvironmentResponseDto>builder()
                .content(dtoPage.getContent())
                .currentPage(dtoPage.getNumber() + 1)
                .pageSize(dtoPage.getSize())
                .totalElements(dtoPage.getTotalElements())
                .totalPages(dtoPage.getTotalPages())
                .first(dtoPage.isFirst())
                .last(dtoPage.isLast())
                .build();

        logger.info("[{}] [{}] Listagem de configurações concluída. Total de elementos: {}, Total de páginas: {}", serviceName, operation, dtoPage.getTotalElements(), dtoPage.getTotalPages());

        String message = buildListMessage(pageableDto, "ambiente");

        return ResponseDto.<PageableDto<EnvironmentResponseDto>>builder()
                .data(pageableDto)
                .message(message)
                .success(true)
                .statusCode(200)
                .build();
    }

    private String buildListMessage(PageableDto<?> pageableDto, String entityName) {
        long totalElements = pageableDto.totalElements();
        int currentPage = pageableDto.currentPage();
        int totalPages = pageableDto.totalPages();
        int pageSize = pageableDto.pageSize();

        if (totalElements == 0) {
            return String.format(
                    "Nenhum %s encontrado. Página %d de %d, Tamanho da página: %d",
                    entityName, currentPage, totalPages, pageSize
            );
        } else if (totalElements == 1) {
            return String.format(
                    "1 %s encontrado. Página %d de %d, Tamanho da página: %d",
                    entityName, currentPage, totalPages, pageSize
            );
        } else {
            return String.format(
                    "%d %ss encontrados. Página %d de %d, Tamanho da página: %d",
                    totalElements, entityName, currentPage, totalPages, pageSize
            );
        }
    }

    @Transactional
    public ResponseDto<EnvironmentResponseDto> updateEnvironment(UUID id, EnvironmentUpdateDto dto) {
        String serviceName = "EnvironmentService";
        String operation = "UPDATE_ENVIRONMENT";

        logger.info("[{}] [{}] Iniciando atualização de ambiente. ID: {}, Nome: {}", serviceName, operation, id, dto.name());

        EnvironmentEntity environment = environmentRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("[{}] [{}] Ambiente não encontrado para atualização - ID: {}", serviceName, operation, id);
                    return new EntityNotFoundException("Ambiente não encontrado");
                });

        logger.debug("[{}] [{}] Ambiente encontrado.  ID: {}, Nome: {}", serviceName, operation, environment.getId(), environment.getName());

        if (!environment.getName().equals(dto.name()) && environmentRepository.existsByName(dto.name())) {
            logger.warn("[{}] [{}] Nome de ambiente duplicado detectado - Nome: {}", serviceName, operation, dto.name());
            throw new DataIntegrityViolationException("Ambiente com nome já existente");
        }

        environment.setName(dto.name());

        logger.debug("[{}] [{}] Persistindo atualização no banco. ID: {}, Novo Nome: {}", serviceName, operation, environment.getId(), dto.name());

        environmentRepository.save(environment);

        EnvironmentResponseDto responseDto = toResponseDto(environment);

        logger.info("[{}] [{}] Ambiente atualizado com sucesso - ID: {}, Nome: {}", serviceName, operation, environment.getId(), environment.getName());

        return ResponseDto.<EnvironmentResponseDto>builder()
                .data(responseDto)
                .message(String.format("Ambiente '%s' atualizado com sucesso", environment.getName()))
                .success(true)
                .statusCode(200)
                .build();
    }

    @Transactional(readOnly = true)
    public ResponseDto<EnvironmentResponseDto> getEnvironmentById(UUID id) {
        String serviceName = "ConfigService";
        String operation = "GET_ENVIRONMENT_BY_ID";

        logger.info("[{}] [{}] Iniciando busca de ambiente por ID: {}", serviceName, operation, id);

        EnvironmentEntity environment = environmentRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("[{}] [{}] Ambiente não encontrado - ID: {}", serviceName, operation, id);
                    return new EntityNotFoundException("Ambiente não encontrado");
                });

        EnvironmentResponseDto responseDto = toResponseDto(environment);

        logger.info("[{}] [{}] Ambiente encontrado com sucesso - ID: {}, Nome: {}", serviceName, operation, environment.getId(), environment.getName());

        return ResponseDto.<EnvironmentResponseDto>builder()
                .data(responseDto)
                .message(String.format("Ambiente '%s' encontrado com sucesso", environment.getName()))
                .success(true)
                .statusCode(200)
                .build();
    }

    public ResponseDto<Void> deleteEnvironment(UUID id) {
        String serviceName = "ConfigService";
        String operation = "DELETE_ENVIRONMENT";

        logger.info("[{}] [{}] Iniciando exclusão de ambiente - ID: {}", serviceName, operation, id);

        EnvironmentEntity environment = environmentRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("[{}] [{}] Ambiente não encontrado para exclusão - ID: {}", serviceName, operation, id);
                    return new EntityNotFoundException("Ambiente não encontrado");
                });

        environmentRepository.delete(environment);

        logger.info("[{}] [{}] Ambiente excluído com sucesso - ID: {}, Nome: {}", serviceName, operation, environment.getId(), environment.getName());

        return ResponseDto.<Void>builder()
                .message(String.format("Ambiente '%s' excluído com sucesso", environment.getName()))
                .success(true)
                .statusCode(200)
                .build();
    }

}
