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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public ResponseDto<NamespaceResponseDto> createNamespace(NamespaceCreateDto dto) {
        String serviceName = "NamespaceService";
        String operation = "CREATE_NAMESPACE";
        String namespaceName = dto.name();

        logger.info("[{}] [{}] Iniciando criação de namespace - Nome: {}", serviceName, operation, namespaceName);

        logger.debug("[{}] [{}] Validando se o nome do namespace é válido - Nome: {}", serviceName, operation, namespaceName);

        if (namespaceRepository.existsByName(dto.name())) {
            logger.warn("[{}] [{}] Namespace duplicado detectado. Nome: {}", serviceName, operation, namespaceName);
            throw new DataIntegrityViolationException(
                    String.format("Namespace com nome '%s' já existe", namespaceName)
            );
        }

        NamespaceEntity namespace = buildNamespace(dto);

        logger.debug("[{}] [{}] Persistindo namespace no banco.  ID: {}, Nome: {}", serviceName, operation, namespace.getId(), namespace.getName());

        namespaceRepository.save(namespace);

        NamespaceResponseDto responseDto = toResponseDto(namespace);

        logger.info("[{}] [{}] Namespace criado com sucesso - ID: {}, Nome: {}", serviceName, operation, namespace.getId(), namespace.getName());

        return ResponseDto.<NamespaceResponseDto>builder()
                .data(responseDto)
                .message(String.format("Namespace '%s' criado com sucesso", namespaceName))
                .success(true)
                .statusCode(201)
                .build();
    }

    private NamespaceEntity buildNamespace(NamespaceCreateDto dto) {
        logger.debug("Construindo entidade Namespace - Nome: {}", dto.name());
        return NamespaceEntity.builder()
                .name(dto.name())
                .build();
    }

    @Transactional(readOnly = true)
    public ResponseDto<PageableDto<NamespaceResponseDto>> listNamespaces(Pageable pageable) {
        String serviceName = "NamespaceService";
        String operation = "LIST_NAMESPACES";

        logger.info("[{}] [{}] Iniciando listagem de namespaces com paginação. Página: {}, Tamanho: {}", serviceName, operation, pageable.getPageNumber(), pageable.getPageSize());

        Page<NamespaceResponseDto> dtoPage = namespaceRepository.findAll(pageable)
                .map(this::toResponseDto);

        PageableDto<NamespaceResponseDto> pageableDto = PageableDto.<NamespaceResponseDto>builder()
                .content(dtoPage.getContent())
                .currentPage(dtoPage.getNumber() + 1)
                .pageSize(dtoPage.getSize())
                .totalElements(dtoPage.getTotalElements())
                .totalPages(dtoPage.getTotalPages())
                .first(dtoPage.isFirst())
                .build();

        logger.info("[{}] [{}] Listagem de namespaces concluída. Total de elementos: {}, Total de páginas: {}", serviceName, operation, pageableDto.totalElements(), pageableDto.totalPages());

        String message = buildListMessage(pageableDto, "namespace");

        return ResponseDto.<PageableDto<NamespaceResponseDto>>builder()
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

    private NamespaceResponseDto toResponseDto(NamespaceEntity namespace) {
        return new NamespaceResponseDto(
                namespace.getId(),
                namespace.getName(),
                namespace.getCreatedAt(),
                namespace.getUpdatedAt()
        );
    }

    @Transactional
    public ResponseDto<NamespaceResponseDto> updateNamespace(UUID id, NamespaceUpdateDto dto) {
        String serviceName = "NamespaceService";
        String operation = "UPDATE_NAMESPACE";

        logger.info("[{}] [{}] Iniciando atualização de namespace - ID: {}, Novo Nome: {}", serviceName, operation, id, dto.name());

        NamespaceEntity namespace = namespaceRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("[{}] [{}] Namespace não encontrado para atualização - ID: {}", serviceName, operation, id);
                    return new EntityNotFoundException("Namespace não encontrado");
                });

        logger.debug("[{}] [{}] Namespace encontrado para atualização - ID: {}, Nome Atual: {}", serviceName, operation, namespace.getId(), namespace.getName());

        logger.debug("[{}] [{}] Validando se o novo nome do namespace é válido - ID: {}, Novo Nome: {}", serviceName, operation, id, dto.name());
        if (!namespace.getName().equals(dto.name()) && namespaceRepository.existsByName(dto.name())) {
            logger.warn("[{}] [{}] Namespace duplicado detectado durante atualização. ID: {}, Novo Nome: {}", serviceName, operation, id, dto.name());
            throw new DataIntegrityViolationException(
                    String.format("Namespace com nome '%s' já existe", dto.name())
            );
        }

        namespace.setName(dto.name());

        logger.debug("[{}] [{}] Persistindo atualização no banco. ID: {}, Novo Nome: {}", serviceName, operation, namespace.getId(), namespace.getName());

        namespaceRepository.save(namespace);

        NamespaceResponseDto responseDto = toResponseDto(namespace);

        logger.info("[{}] [{}] Namespace atualizado com sucesso - ID: {}, Novo Nome: {}", serviceName, operation, namespace.getId(), namespace.getName());

        return ResponseDto.<NamespaceResponseDto>builder()
                .data(responseDto)
                .message(String.format("Namespace '%s' atualizado com sucesso", namespace.getName()))
                .success(true)
                .statusCode(200)
                .build();
    }

    @Transactional(readOnly = true)
    public ResponseDto<NamespaceResponseDto> getNamespaceById(UUID id) {
        String serviceName = "NamespaceService";
        String operation = "GET_NAMESPACE_BY_ID";

        logger.info("[{}] [{}] Iniciando busca de namespace por ID - ID: {}", serviceName, operation, id);

        NamespaceEntity namespace = namespaceRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("[{}] [{}] Namespace não encontrado - ID: {}", serviceName, operation, id);
                    return new EntityNotFoundException("Namespace não encontrado");
                });

        NamespaceResponseDto responseDto = toResponseDto(namespace);

        logger.info("[{}] [{}] Namespace encontrado com sucesso - ID: {}, Nome: {}", serviceName, operation, namespace.getId(), namespace.getName());

        return ResponseDto.<NamespaceResponseDto>builder()
                .data(responseDto)
                .message(String.format("Namespace '%s' encontrado com sucesso", namespace.getName()))
                .success(true)
                .statusCode(200)
                .build();
    }

    public ResponseDto<Void> deleteNamespace(UUID id) {
        String serviceName = "NamespaceService";
        String operation = "DELETE_NAMESPACE";

        logger.info("[{}] [{}] Iniciando exclusão de namespace - ID: {}", serviceName, operation, id);

        NamespaceEntity namespace = namespaceRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("[{}] [{}] Namespace não encontrado para exclusão - ID: {}", serviceName, operation, id);
                    return new EntityNotFoundException("Namespace não encontrado");
                });

        logger.debug("[{}] [{}] Namespace encontrado para exclusão - ID: {}, Nome: {}", serviceName, operation, namespace.getId(), namespace.getName());

        boolean hasEnvironments = environmentRepository.existsByNamespaceId(id);
        if (hasEnvironments) {
            logger.warn("[{}] [{}] Não é possível excluir o namespace porque ele possui ambientes vinculados - ID: {}, Nome: {}", serviceName, operation, namespace.getId(), namespace.getName());
            throw new DataIntegrityViolationException("Não é possível excluir o namespace porque ele possui ambientes vinculados");
        }

        namespaceRepository.delete(namespace);

        logger.info("[{}] [{}] Namespace excluído com sucesso - ID: {}, Nome: {}", serviceName, operation, namespace.getId(), namespace.getName());

        return ResponseDto.<Void>builder()
                .data(null)
                .message(String.format("Namespace '%s' excluído com sucesso", namespace.getName()))
                .success(true)
                .statusCode(200)
                .build();
    }

    @Transactional(readOnly = true)
    public ResponseDto<PageableDto<EnvironmentSimpleResponseDto>> getEnvironmentsByNamespaceId(UUID id, Pageable pageable) {
        String serviceName = "NamespaceService";
        String operation = "GET_ENVIRONMENTS_BY_NAMESPACE_ID";

        logger.info("[{}] [{}] Iniciando busca de ambientes vinculados ao namespace - Namespace ID: {}, Página: {}, Tamanho: {}", serviceName, operation, id, pageable.getPageNumber(), pageable.getPageSize());

        NamespaceEntity namespace = namespaceRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("[{}] [{}] Namespace não encontrado - ID: {}", serviceName, operation, id);
                    return new EntityNotFoundException("Namespace não encontrado");
                });

        Page<EnvironmentSimpleResponseDto> dtoPage = environmentRepository.findByNamespaceId(id, pageable)
                .map(env -> new EnvironmentSimpleResponseDto(
                        env.getId(),
                        env.getName(),
                        env.getCreatedAt(),
                        env.getUpdatedAt()
                ));

        PageableDto<EnvironmentSimpleResponseDto> pageableDto = PageableDto.<EnvironmentSimpleResponseDto>builder()
                .content(dtoPage.getContent())
                .currentPage(dtoPage.getNumber() + 1)
                .pageSize(dtoPage.getSize())
                .totalElements(dtoPage.getTotalElements())
                .totalPages(dtoPage.getTotalPages())
                .first(dtoPage.isFirst())
                .last(dtoPage.isLast())
                .build();

        logger.info("[{}] [{}] Busca de ambientes concluída. Total de elementos: {}, Total de páginas: {}", serviceName, operation, pageableDto.totalElements(), pageableDto.totalPages());


        String message = buildListMessage(pageableDto, "ambiente");

        return ResponseDto.<PageableDto<EnvironmentSimpleResponseDto>>builder()
                .data(pageableDto)
                .message(message)
                .success(true)
                .statusCode(200)
                .build();
    }


}



