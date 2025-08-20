package com.example.config_service_api.service;

import com.example.config_service_api.config.ConfigChangeEvent;
import com.example.config_service_api.dto.*;
import com.example.config_service_api.entity.DataEntity;
import com.example.config_service_api.entity.EnvironmentEntity;
import com.example.config_service_api.entity.HistoryConfigEntity;
import com.example.config_service_api.enums.OperationType;
import com.example.config_service_api.repository.DataRepository;
import com.example.config_service_api.repository.EnvironmentRepository;
import com.example.config_service_api.repository.HistoryConfigRepository;
import com.example.config_service_api.utils.CustomCacheManager;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DataConfigService {

    private static final Logger logger = LoggerFactory.getLogger(DataConfigService.class);

    private final DataRepository dataRepository;
    private final EnvironmentRepository environmentRepository;
    private final HistoryConfigRepository historyConfigRepository;
    private final ConfigEventProducer configEventProducer;
    private final CustomCacheManager customCacheManager;
    private final StringRedisTemplate stringRedisTemplate;

    public DataConfigService(DataRepository dataRepository, EnvironmentRepository environmentRepository, HistoryConfigRepository historyConfigRepository, ConfigEventProducer configEventProducer, CustomCacheManager customCacheManager, StringRedisTemplate stringRedisTemplate) {
        this.dataRepository = dataRepository;
        this.environmentRepository = environmentRepository;
        this.historyConfigRepository = historyConfigRepository;
        this.configEventProducer = configEventProducer;
        this.customCacheManager = customCacheManager;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Cria uma nova configuração de dados.
     */
    @Transactional
    public ResponseDto<DataConfigResponseDto> createDataConfig(DataConfigCreateDto dto) {
        String serviceName = "ConfigService";
        String operation = "CREATE_DATA_CONFIG";

        EnvironmentEntity environment = findEnvironment(dto.environmentId(), serviceName, operation);
        validateDuplicateConfig(dto.key(), dto.environmentId(), serviceName, operation);

        DataEntity dataEntity = buildDataEntity(dto, environment);
        dataRepository.save(dataEntity);

        sendConfigEventToKafka(dataEntity, "CREATE", serviceName, operation);

        saveHistoryConfig(dataEntity, OperationType.CREATE, null, dataEntity.getValue(), null, dataEntity.getKey());

        DataConfigResponseDto responseDto = toResponseDto(dataEntity);

        String cacheKey = "CONFIG_CACHE::config_" + responseDto.id() + "_" + responseDto.key();
        customCacheManager.saveToCache(cacheKey, responseDto);

        logger.info("[{}] [{}] Configuração criada com sucesso. ID: {}, Chave: {}, Ambiente: {}",
                serviceName, operation, dataEntity.getId(), dto.key(), dto.environmentId());

        return ResponseDto.<DataConfigResponseDto>builder()
                .data(responseDto)
                .message(String.format("Configuração '%s' criada com sucesso no ambiente com ID: %s", dto.key(), dto.environmentId()))
                .success(true)
                .statusCode(201)
                .build();
    }

    /**
     * Lista todas as configuraçõescom paginação.
     */
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

    /**
     * Lista as configurações  por ambiente com paginação.
     */
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

    /**
     * Atualiza uma configuração  existente.
     */
    @Transactional
    public ResponseDto<DataConfigResponseDto> updateDataConfig(UUID id, DataConfigUpdateDto dto) {
        String serviceName = "ConfigService";
        String operation = "UPDATE_DATA_CONFIG";

        logger.info("[{}] [{}] Iniciando atualização de configuração. ID: {}, Chave: {}", serviceName, operation, id, dto.key());

        DataEntity dataEntity = findDataEntityById(id, serviceName, operation);

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

        if (!oldKey.equals(newKey)) {
            String oldCacheKey = "CONFIG_CACHE::config_" + id + "_" + oldKey;
            customCacheManager.deleteFromCache(oldCacheKey);
            logger.info("[{}] [{}] Chave antiga invalidada: {}", serviceName, operation, oldCacheKey);
        }

        applyUpdates(dataEntity, dto, serviceName, operation);
        dataRepository.save(dataEntity);

        logger.info("[{}] [{}] Persistindo histórico da configuração atualizada. ID: {}, Chave: {}", serviceName, operation, dataEntity.getId(), dataEntity.getKey());
        saveHistoryConfig(dataEntity, OperationType.UPDATE, oldValue, newValue, oldKey, newKey);
        logger.debug("[{}] [{}] Histórico da configuração atualizado com sucesso. ID: {}, Chave: {}", serviceName, operation, dataEntity.getId(), dataEntity.getKey());

        DataConfigResponseDto responseDto = toResponseDto(dataEntity);

        String newCacheKey = "CONFIG_CACHE::config_" + id + "_" + dataEntity.getKey();
        customCacheManager.saveToCache(newCacheKey, responseDto);
        logger.info("[{}] [{}] Nova chave adicionada ao cache: {}", serviceName, operation, newCacheKey);

        logger.info("[{}] [{}] Configuração atualizada com sucesso. ID: {}, Chave: {}", serviceName, operation, id, dataEntity.getKey());

        sendConfigEventToKafka(dataEntity, "UPDATE", serviceName, operation);

        return ResponseDto.<DataConfigResponseDto>builder()
                .data(responseDto)
                .message(String.format("Configuração com a chave '%s' atualizada com sucesso", dataEntity.getKey()))
                .success(true)
                .statusCode(200)
                .build();
    }

    /**
     * Busca uma configuração por ID.
     */
    @Transactional(readOnly = true)
    public ResponseDto<DataConfigResponseDto> getDataConfigById(UUID id) {
        String serviceName = "ConfigService";
        String operation = "GET_DATA_CONFIG_BY_ID";

        logger.info("[{}] [{}] Buscando configuração por ID. ID: {}", serviceName, operation, id);

        String cachePattern = "CONFIG_CACHE::config_" + id + "_*";
        DataConfigResponseDto cached = customCacheManager.getFromCache(cachePattern, DataConfigResponseDto.class);

        if (cached != null) {
            logger.info("[{}] [{}] Cache HIT", serviceName, operation);
            return buildSuccessResponse(cached, "Dados do cache");
        }

        logger.info("[{}] [{}] Cache MISS. Buscando no banco...", serviceName, operation);


        DataEntity dataEntity = findDataEntityById(id, serviceName, operation);
        DataConfigResponseDto responseDto = toResponseDto(dataEntity);


        String cacheKey = "CONFIG_CACHE::config_" + id + "_" + dataEntity.getKey();
        customCacheManager.saveToCache(cacheKey, responseDto);

        return buildSuccessResponse(responseDto,
                String.format("Configuração com a chave '%s' encontrada", dataEntity.getKey()));
    }

    private ResponseDto<DataConfigResponseDto> buildSuccessResponse(DataConfigResponseDto data, String message) {
        return ResponseDto.<DataConfigResponseDto>builder()
                .data(data)
                .message(message)
                .success(true)
                .statusCode(200)
                .build();
    }

    /**
     * Exclui uma configuração por ID.
     */
    public ResponseDto<Void> deleteDataConfig(UUID id) {
        String serviceName = "ConfigService";
        String operation = "DELETE_DATA_CONFIG";

        logger.info("[{}] [{}] Iniciando exclusão de configuração. ID: {}", serviceName, operation, id);

        DataEntity dataEntity = dataRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("[{}] [{}] Configuração não encontrada para exclusão - ID: {}", serviceName, operation, id);
                    return new EntityNotFoundException("Configuração não encontrada");
                });

        String cachePattern = "CONFIG_CACHE::config_" + id + "_*";
        customCacheManager.deleteFromCache(cachePattern);
        logger.info("[{}] [{}] Cache invalidado para padrão: {}", serviceName, operation, cachePattern);

        logger.info("[{}] [{}] Persistindo histórico da configuração excluída. ID: {}, Chave: {}", serviceName, operation, dataEntity.getId(), dataEntity.getKey());
        saveHistoryConfig(dataEntity, OperationType.DELETE, dataEntity.getValue(), null, dataEntity.getKey(), null);

        dataRepository.delete(dataEntity);

        sendConfigEventToKafka(dataEntity, "DELETE", serviceName, operation);

        logger.info("[{}] [{}] Configuração excluída com sucesso. ID: {}", serviceName, operation, id);

        return ResponseDto.<Void>builder()
                .message(String.format("Configuração com a chave '%s' excluída com sucesso", dataEntity.getKey()))
                .success(true)
                .statusCode(200)
                .build();
    }


    /**
     * Busca o histórico de configurações por ambiente com paginação.
     */
    @Transactional(readOnly = true)
    public ResponseDto<PageableDto<HistoryConfigResponseDto>> getHistoryByEnvironment(UUID environmentId, Pageable pageable) {
        String serviceName = "ConfigService";
        String operation = "GET_HISTORY_BY_ENVIRONMENT";

        logger.info("[{}] [{}] Iniciando busca de histórico por ambiente. Ambiente ID: {}", serviceName, operation, environmentId);

        Page<HistoryConfigEntity> historyEntities = fetchHistoryEntities(environmentId, pageable, serviceName, operation);

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

    // => Auxilia buscar as entidades de histórico
    private Page<HistoryConfigEntity> fetchHistoryEntities(UUID environmentId, Pageable pageable, String serviceName, String operation) {
        Page<HistoryConfigEntity> historyEntities = historyConfigRepository.findByEnvironmentId(environmentId, pageable);

        if (historyEntities.isEmpty()) {
            logger.warn("[{}] [{}] Nenhum histórico encontrado para o ambiente ID: {}", serviceName, operation, environmentId);
            throw new EntityNotFoundException(String.format("Nenhum histórico encontrado para o ambiente de ID: %s", environmentId));
        }

        return historyEntities;
    }


    // => Auxilia mapear HistoryConfigEntity para HistoryConfigResponseDto
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

    // => Auxilia construir o PageableDto a partir do Page de HistoryConfigEntity
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

    // => Auxilia verificar se há atualizações a serem aplicadas
    private boolean hasUpdate(DataConfigUpdateDto dto) {
        return (dto.key() != null && !dto.key().isBlank()) || (dto.value() != null && !dto.value().isBlank());
    }

    // => Auxilia aplicar as atualizações no DataEntity
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

    // => Auxilia validar a unicidade da chave
    private void validateKeyUniqueness(DataEntity entity, String newKey, String serviceName, String operation) {
        if (!entity.getKey().equals(newKey) &&
                dataRepository.existsByKeyAndEnvironmentId(newKey, entity.getEnvironment().getId())) {
            logger.debug("[{}] [{}] Configuração duplicada detectada. Chave: {}, Ambiente ID: {}", serviceName, operation, newKey, entity.getEnvironment().getId());
            throw new DataIntegrityViolationException(
                    String.format("Configuração com chave '%s' já existe no ambiente %s", newKey, entity.getEnvironment().getId())
            );
        }
    }

    // => Auxilia construir a mensagem de listagem
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

    // => Auxilia encontrar o ambiente
    private EnvironmentEntity findEnvironment(UUID environmentId, String serviceName, String operation) {
        logger.debug("[{}] [{}] Buscando ambiente no repositório. Ambiente ID: {}", serviceName, operation, environmentId);
        return environmentRepository.findById(environmentId)
                .orElseThrow(() -> new EntityNotFoundException("Ambiente não encontrado"));
    }

    // => Auxilia encontrar uma Config por ID
    private DataEntity findDataEntityById(UUID id, String serviceName, String operation) {
        logger.debug("[{}] [{}] Buscando Config no repositório. ID: {}", serviceName, operation, id);
        return dataRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("[{}] [{}] Configuração não encontrada. ID: {}", serviceName, operation, id);
                    return new EntityNotFoundException("Configuração não encontrada");
                });
    }

    // => Auxilia validar a duplicidade da configuração
    private void validateDuplicateConfig(String key, UUID environmentId, String serviceName, String operation) {
        if (dataRepository.existsByKeyAndEnvironmentId(key, environmentId)) {
            logger.debug("[{}] [{}] Configuração duplicada detectada. Chave: {}, Ambiente ID: {}", serviceName, operation, key, environmentId);
            throw new DataIntegrityViolationException(
                    String.format("Configuração com chave '%s' já existe no ambiente do ID: %s", key, environmentId)
            );
        }
    }

    // => Auxilia salvar o histórico da configuração
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

    // => Auxilia enviar o evento de configuração para o Kafka
    private void sendConfigEventToKafka(DataEntity dataEntity, String action, String serviceName, String operation) {
        logger.info("[{}] [{}] Enviando evento '{}' para Kafka. Config ID: {}, Chave: {}",
                serviceName, operation, action, dataEntity.getId(), dataEntity.getKey());

        ConfigChangeEvent event = new ConfigChangeEvent(
                action,
                dataEntity.getId(),
                dataEntity.getEnvironment().getNamespace().getName(),
                dataEntity.getEnvironment().getName(),
                dataEntity.getKey(),
                dataEntity.getValue(),
                LocalDateTime.now()
        );

        try {
            configEventProducer.sendConfigChange(event);
            logger.info("[{}] [{}] Evento '{}' enviado para Kafka com sucesso. Config ID: {}, Chave: {}",
                    serviceName, operation, action, dataEntity.getId(), dataEntity.getKey());
        } catch (Exception ex) {
            logger.error("[{}] [{}] Falha ao enviar evento '{}' para Kafka. Config ID: {}, Chave: {}. Erro: {}",
                    serviceName, operation, action, dataEntity.getId(), dataEntity.getKey(), ex.getMessage(), ex);
        }
    }

    // => Auxilia construir o DataEntity a partir do DTO
    private DataEntity buildDataEntity(DataConfigCreateDto dto, EnvironmentEntity environment) {
        logger.debug("Construindo DataEntity a partir do DTO. Chave: {}, Valor: {}", dto.key(), dto.value());
        return DataEntity.builder()
                .key(dto.key())
                .value(dto.value())
                .environment(environment)
                .build();
    }

    // => Auxilia converter DataEntity para DataConfigResponseDto
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

}
