package com.example.config_service_api.service;

import com.example.config_service_api.dto.*;
import com.example.config_service_api.entity.DataEntity;
import com.example.config_service_api.entity.EnvironmentEntity;
import com.example.config_service_api.entity.HistoryConfigEntity;
import com.example.config_service_api.enums.OperationType;
import com.example.config_service_api.repository.DataRepository;
import com.example.config_service_api.repository.EnvironmentRepository;
import com.example.config_service_api.repository.HistoryConfigRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.core.Local;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DataConfigService {

    private static final Logger logger = LoggerFactory.getLogger(DataConfigService.class);

    private final DataRepository dataRepository;
    private final EnvironmentRepository environmentRepository;
    private final HistoryConfigRepository historyConfigRepository;

    public DataConfigService(DataRepository dataRepository, EnvironmentRepository environmentRepository, HistoryConfigRepository historyConfigRepository) {
        this.dataRepository = dataRepository;
        this.environmentRepository = environmentRepository;
        this.historyConfigRepository = historyConfigRepository;
    }

    @CachePut(value = "CONFIG_CACHE", key = "#result.id()")
    @Transactional
    public ResponseDto<DataConfigResponseDto> createDataConfig(DataConfigCreateDto dto) {
        String serviceName = "ConfigService";
        String operation = "CREATE_DATA_CONFIG";
        UUID environmentId = dto.environmentId();
        String key = dto.key();

        logger.info("[{}] [{}] Iniciando criação de configuração. Chave: {}, Ambiente: {}", serviceName, operation, key, environmentId);

        logger.debug("[{}] [{}] Buscando ambiente no repositório. Ambiente ID: {}", serviceName, operation, environmentId);
        EnvironmentEntity environment = environmentRepository.findById(dto.environmentId())
                .orElseThrow(() -> new EntityNotFoundException("Ambiente não encontrado"));

        logger.info("[{}] [{}] Ambiente encontrado. ID: {}, Nome: {}", serviceName, operation, environment.getId(), environment.getName());

        if (dataRepository.existsByKeyAndEnvironmentId(key, dto.environmentId())) {
            logger.debug("[{}] [{}] Configuração duplicada detectada. Chave: {}, Ambiente ID: {}", serviceName, operation, key, environmentId);
            throw new DataIntegrityViolationException(
                    String.format("Configuração com chave '%s' já existe no ambiente do ID: %s", key, environmentId)
            );
        }

        DataEntity dataEntity = buildDataEntity(dto, environment);

        logger.debug("[{}] [{}] Persistindo configuração no banco. Chave: {}, Ambiente: {}", serviceName, operation, key, environmentId);

        dataRepository.save(dataEntity);

        logger.info("[{}] [{}] Persistindo histórico da configuração. ID: {}, Chave: {}, Ambiente: {}", serviceName, operation, dataEntity.getId(), key, environmentId);

        saveHistoryConfig(dataEntity, OperationType.CREATE, null, dataEntity.getValue(), null, dataEntity.getKey());

        logger.debug("[{}] [{}] Histórico da configuração persistido com sucesso. ID: {}, Chave: {}, Ambiente: {}", serviceName, operation, dataEntity.getId(), key, environmentId);

        DataConfigResponseDto responseDto = toResponseDto(dataEntity);

        logger.info("[{}] [{}] Configuração criada com sucesso. ID: {}, Chave: {}, Ambiente: {}", serviceName, operation, dataEntity.getId(), key, environmentId);

        return ResponseDto.<DataConfigResponseDto>builder()
                .data(responseDto)
                .message(String.format("Configuração '%s' criada com sucesso no ambiente com ID: %s", key, environmentId))
                .success(true)
                .statusCode(201)
                .build();
    }

    private void saveHistoryConfig(DataEntity entity, OperationType type, String oldValue, String newValue, String oldKey, String newKey) {
        HistoryConfigEntity historyConfigEntity = HistoryConfigEntity.builder()
                .configurationId(type == OperationType.DELETE ? null : entity.getId())
                .environmentId(entity.getEnvironment().getId())
                .oldKey(oldKey)
                .newKey(newKey)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedBy("system")
                .changedAt(LocalDateTime.now())
                .operationType(type)
                .build();

        historyConfigRepository.save(historyConfigEntity);
    }

    private DataEntity buildDataEntity(DataConfigCreateDto dto, EnvironmentEntity environment) {
        logger.debug("Construindo DataEntity a partir do DTO. Chave: {}, Valor: {}", dto.key(), dto.value());
        return DataEntity.builder()
                .key(dto.key())
                .value(dto.value())
                .environment(environment)
                .build();
    }

    public DataConfigResponseDto toResponseDto(DataEntity dataEntity) {
        return new DataConfigResponseDto(
                dataEntity.getId(),
                dataEntity.getKey(),
                dataEntity.getValue(),
                dataEntity.getCreatedAt(),
                dataEntity.getUpdatedAt(),
                dataEntity.getEnvironment().getId()
        );
    }

    @Cacheable(value = "CONFIG_CACHE", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public ResponseDto<PageableDto<DataConfigResponseDto>> listDataConfigs(Pageable pageable) {
        String serviceName = "ConfigService";
        String operation = "LIST_DATA_CONFIGS";

        logger.info("[{}] [{}] Iniciando listagem de configurações com paginação. Página: {}, Tamanho: {}", serviceName, operation, pageable.getPageNumber(), pageable.getPageSize());

        Page<DataConfigResponseDto> dtoPage = dataRepository.findAll(pageable)
                .map(this::toResponseDto);


        PageableDto<DataConfigResponseDto> pageableDto = PageableDto.<DataConfigResponseDto>builder()
                .content(dtoPage.getContent())
                .currentPage(dtoPage.getNumber() + 1)
                .pageSize(dtoPage.getSize())
                .totalElements(dtoPage.getTotalElements())
                .totalPages(dtoPage.getTotalPages())
                .first(dtoPage.isFirst())
                .last(dtoPage.isLast())
                .build();

        logger.info("[{}] [{}] Listagem de configurações concluída. Total de elementos: {}, Total de páginas: {}", serviceName, operation, dtoPage.getTotalElements(), dtoPage.getTotalPages());

        String message = buildListMessage(pageableDto, "configuração", "configurações");


        return ResponseDto.<PageableDto<DataConfigResponseDto>>builder()
                .data(pageableDto)
                .message(message)
                .success(true)
                .statusCode(200)
                .build();
    }

    @Cacheable(value = "CONFIG_CACHE", key = "#environmentId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public ResponseDto<PageableDto<DataConfigResponseDto>> listDataConfigsByEnvironment(UUID environmentId, Pageable pageable) {
        String serviceName = "ConfigService";
        String operation = "LIST_DATA_CONFIGS_BY_ENVIRONMENT";

        logger.info("[{}] [{}] Iniciando listagem de configurações por ambiente. Ambiente ID: {}, Página: {}, Tamanho: {}",
                serviceName, operation, environmentId, pageable.getPageNumber(), pageable.getPageSize());

        Page<DataConfigResponseDto> dataPage = dataRepository.findByEnvironmentId(environmentId, pageable)
                .map(this::toResponseDto);

        if (dataPage.isEmpty()) {
            logger.warn("[{}] [{}] Nenhuma configuração encontrada para o ambiente ID: {}", serviceName, operation, environmentId);
            throw new EntityNotFoundException(String.format("Nenhuma configuração encontrada para o ambiente de ID: %s", environmentId));
        }

        PageableDto<DataConfigResponseDto> pageableDto = PageableDto.<DataConfigResponseDto>builder()
                .content(dataPage.getContent())
                .currentPage(dataPage.getNumber() + 1)
                .pageSize(dataPage.getSize())
                .totalElements(dataPage.getTotalElements())
                .totalPages(dataPage.getTotalPages())
                .first(dataPage.isFirst())
                .last(dataPage.isLast())
                .build();

        String message = buildListMessage(pageableDto, "configuração", "configurações");

        logger.info("[{}] [{}] Listagem de configurações concluída. Total de elementos: {}, Total de páginas: {}",
                serviceName, operation, dataPage.getTotalElements(), dataPage.getTotalPages());

        return ResponseDto.<PageableDto<DataConfigResponseDto>>builder()
                .data(pageableDto)
                .message(message)
                .success(true)
                .statusCode(200)
                .build();
    }

    private String buildListMessage(PageableDto<?> pageableDto, String singular, String plural) {
        long totalElements = pageableDto.totalElements();
        int currentPage = pageableDto.currentPage();
        int totalPages = pageableDto.totalPages();
        int pageSize = pageableDto.pageSize();

        if (totalElements == 0) {
            return String.format("Nenhum %s encontrada. Página %d de %d, Tamanho da página: %d",
                    singular, currentPage, totalPages, pageSize);
        } else if (totalElements == 1) {
            return String.format("1 %s encontrada. Página %d de %d, Tamanho da página: %d",
                    singular, currentPage, totalPages, pageSize);
        } else {
            return String.format("%d %s encontradas. Página %d de %d, Tamanho da página: %d",
                    totalElements, plural, currentPage, totalPages, pageSize);
        }
    }

    @CachePut(value = "CONFIG_CACHE", key = "#result.id()")
    @Transactional
    public ResponseDto<DataConfigResponseDto> updateDataConfig(UUID id, DataConfigUpdateDto dto) {
        String serviceName = "ConfigService";
        String operation = "UPDATE_DATA_CONFIG";

        logger.info("[{}] [{}] Iniciando atualização de configuração. ID: {}, Chave: {}", serviceName, operation, id, dto.key());

        DataEntity dataEntity = dataRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("[{}] [{}] Configuração não encontrada. ID: {}", serviceName, operation, id);
                    return new EntityNotFoundException("Configuração não encontrada");
                });

        String oldKey = dataEntity.getKey();
        String oldValue = dataEntity.getValue();
        String newKey = dto.key();
        String newValue = dto.value();

        if (!hasUpdate(dto)) {
            logger.warn("[{}] [{}] Nenhum dado para atualizar. ID: {}, Chave: {}", serviceName, operation, id, dto.key());
            return ResponseDto.<DataConfigResponseDto>builder()
                    .message("Nenhum dado para atualizar")
                    .success(false)
                    .statusCode(400)
                    .build();
        }

        applyUpdates(dataEntity, dto, serviceName, operation);

        dataRepository.save(dataEntity);

        logger.info("[{}] [{}] Persistindo histórico da configuração atualizada. ID: {}, Chave: {}", serviceName, operation, dataEntity.getId(), dataEntity.getKey());

        saveHistoryConfig(dataEntity, OperationType.UPDATE, oldValue, newValue, oldKey, newKey);

        logger.debug("[{}] [{}] Histórico da configuração atualizado com sucesso. ID: {}, Chave: {}", serviceName, operation, dataEntity.getId(), dataEntity.getKey());

        DataConfigResponseDto responseDto = toResponseDto(dataEntity);

        logger.info("[{}] [{}] Configuração atualizada com sucesso. ID: {}, Chave: {}", serviceName, operation, id, dataEntity.getKey());

        return ResponseDto.<DataConfigResponseDto>builder()
                .data(responseDto)
                .message(String.format("Configuração com a chave '%s' atualizada com sucesso", dataEntity.getKey()))
                .success(true)
                .statusCode(200)
                .build();
    }

    @Cacheable(value = "CONFIG+CACHE", key = "#dataConfigId")
    @Transactional(readOnly = true)
    public ResponseDto<DataConfigResponseDto> getDataConfigById(UUID id) {
        String serviceName = "ConfigService";
        String operation = "GET_DATA_CONFIG_BY_ID";

        logger.info("[{}] [{}] Buscando configuração por ID. ID: {}", serviceName, operation, id);

        DataEntity dataEntity = dataRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("[{}] [{}] Configuração não encontrada. ID: {}", serviceName, operation, id);
                    return new EntityNotFoundException("Configuração não encontrada");
                });

        DataConfigResponseDto responseDto = toResponseDto(dataEntity);

        logger.info("[{}] [{}] Configuração encontrada. ID: {}, Chave: {}", serviceName, operation, id, dataEntity.getKey());

        return ResponseDto.<DataConfigResponseDto>builder()
                .data(responseDto)
                .message(String.format("Configuração com a chave '%s' encontrada", dataEntity.getKey()))
                .success(true)
                .statusCode(200)
                .build();
    }

    @CacheEvict(value ="CONFIG_CACHE", key = "#dataConfigId")
    public ResponseDto<Void> deleteDataConfig(UUID id) {
        String serviceName = "ConfigService";
        String operation = "DELETE_DATA_CONFIG";

        logger.info("[{}] [{}] Iniciando exclusão de configuração. ID: {}", serviceName, operation, id);

        DataEntity dataEntity = dataRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("[{}] [{}] Configuração não encontrada para exclusão - ID: {}", serviceName, operation, id);
                    return new EntityNotFoundException("Configuração não encontrada");
                });

        logger.info("[{}] [{}] Persistindo histórico da configuração excluída. ID: {}, Chave: {}", serviceName, operation, dataEntity.getId(), dataEntity.getKey());

        saveHistoryConfig(dataEntity, OperationType.DELETE, dataEntity.getValue(), null, dataEntity.getKey(), null);

        logger.debug("[{}] [{}] Histórico da configuração excluída persistido com sucesso. ID: {}, Chave: {}", serviceName, operation, dataEntity.getId(), dataEntity.getKey());

        dataRepository.delete(dataEntity);

        logger.info("[{}] [{}] Configuração excluída com sucesso. ID: {}", serviceName, operation, id);

        return ResponseDto.<Void>builder()
                .message(String.format("Configuração com a chave '%s' excluída com sucesso", dataEntity.getKey()))
                .success(true)
                .statusCode(200)
                .build();
    }

    private boolean hasUpdate(DataConfigUpdateDto dto) {
        return (dto.key() != null && !dto.key().isBlank()) || (dto.value() != null && !dto.value().isBlank());
    }

    private void applyUpdates(DataEntity entity, DataConfigUpdateDto dto, String serviceName, String operation) {
        if (dto.key() != null && !dto.key().isBlank() && !dto.key().equals(entity.getKey())) {
            validateKeyUniqueness(entity, dto.key(), serviceName, operation);
            logger.debug("[{}] [{}] Atualizando chave da configuração. ID: {}, De: {}, Para: {}",
                    serviceName, operation, entity.getId(), entity.getKey(), dto.key());
            entity.setKey(dto.key().trim());
        }

        if (dto.value() != null && !dto.value().isBlank() && !dto.value().equals(entity.getValue())) {
            logger.debug("[{}] [{}] Atualizando valor da configuração. ID: {}, De: {}, Para: {}",
                    serviceName, operation, entity.getId(), entity.getValue(), dto.value());
            entity.setValue(dto.value().trim());
        }

    }

    private void validateKeyUniqueness(DataEntity entity, String newKey, String serviceName, String operation) {
        if (!entity.getKey().equals(newKey) &&
                dataRepository.existsByKeyAndEnvironmentId(newKey, entity.getEnvironment().getId())) {
            logger.debug("[{}] [{}] Configuração duplicada detectada. Chave: {}, Ambiente ID: {}", serviceName, operation, newKey, entity.getEnvironment().getId());
            throw new DataIntegrityViolationException(
                    String.format("Configuração com chave '%s' já existe no ambiente %s", newKey, entity.getEnvironment().getId())
            );
        }
    }

    @Cacheable(value = "CONFIG_CACHE", key = "#environmentId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public ResponseDto<PageableDto<HistoryConfigResponseDto>> getHistoryByEnvironment(UUID environmentId, Pageable pageable) {
        String serviceName = "ConfigService";
        String operation = "GET_HISTORY_BY_ENVIRONMENT";

        logger.info("[{}] [{}] Iniciando busca de histórico por ambiente. Ambiente ID: {}", serviceName, operation, environmentId);

        Page<HistoryConfigEntity> historyEntities = fetchHistoryEntities(environmentId, serviceName, operation);

        List<HistoryConfigResponseDto> historyDtos = mapToDto(historyEntities);

        PageableDto<HistoryConfigResponseDto> pageableDto = buildPageableDto(historyEntities, historyDtos);

        logger.info("[{}] [{}] Busca de histórico concluída. Total de elementos: {}", serviceName, operation, historyEntities.getTotalElements());

        return ResponseDto.<PageableDto<HistoryConfigResponseDto>>builder()
                .data(pageableDto)
                .message(String.format("Histórico encontrado para o ambiente com ID: %s", environmentId))
                .success(true)
                .statusCode(200)
                .build();
    }

    private Page<HistoryConfigEntity> fetchHistoryEntities(UUID environmentId, String serviceName, String operation) {
        Page<HistoryConfigEntity> historyEntities = historyConfigRepository.findByEnvironmentId(environmentId, Pageable.unpaged());

        if (historyEntities.isEmpty()) {
            logger.warn("[{}] [{}] Nenhum histórico encontrado para o ambiente ID: {}", serviceName, operation, environmentId);
            throw new EntityNotFoundException(String.format("Nenhum histórico encontrado para o ambiente de ID: %s", environmentId));
        }

        return historyEntities;
    }

    private List<HistoryConfigResponseDto> mapToDto(Page<HistoryConfigEntity> historyEntities) {
        return historyEntities.stream()
                .map(entity -> HistoryConfigResponseDto.fromEntity(
                        entity.getEnvironmentId(),
                        entity.getOldKey(),
                        entity.getNewKey(),
                        entity.getOldValue(),
                        entity.getNewValue(),
                        entity.getChangedBy(),
                        entity.getOperationType(),
                        entity.getChangedAt(),
                        entity.getId()
                ))
                .toList();
    }

    private PageableDto<HistoryConfigResponseDto> buildPageableDto(Page<HistoryConfigEntity> historyEntities, List<HistoryConfigResponseDto> content) {
        return PageableDto.<HistoryConfigResponseDto>builder()
                .content(content)
                .currentPage(historyEntities.getNumber() + 1)
                .pageSize(historyEntities.getSize())
                .totalElements(historyEntities.getTotalElements())
                .totalPages(historyEntities.getTotalPages())
                .first(historyEntities.isFirst())
                .last(historyEntities.isLast())
                .build();
    }


}
