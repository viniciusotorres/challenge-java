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
     * Listagem de configurações por Namespace + environment
     */
    public ResponseDto<PageableDto<DataConfigResponseDto>> listConfigsByNamespaceAndEnv(
            String namespace,
            String environment,
            Pageable pageable) {
        String serviceName = "ConfigService";
        String operation = "LIST_CONFIGS_BY_NAMESPACE_AND_ENV";

        logger.info("[{}] [{}] Iniciando listagem de configurações por namespace e ambiente. Namespace: {}, Ambiente: {}, Página: {}, Tamanho: {}",
                serviceName, operation, namespace, environment, pageable.getPageNumber(), pageable.getPageSize());

        Page<DataConfigResponseDto> dtoPage = dataRepository.findByEnvironmentNamespaceNameAndEnvironmentName(namespace, environment, pageable)
                .map(this::toResponseDto);

        if (dtoPage.isEmpty()) {
            logger.warn("[{}] [{}] Nenhuma configuração encontrada para o namespace '{}' e ambiente '{}'", serviceName, operation, namespace, environment);
            throw new EntityNotFoundException(String.format("Nenhuma configuração encontrada para o namespace '%s' e ambiente '%s'", namespace, environment));
        }

        PageableDto<DataConfigResponseDto> pageableDto = PageableDto.<DataConfigResponseDto>builder()
                .content(dtoPage.getContent())
                .currentPage(dtoPage.getNumber() + 1)
                .pageSize(dtoPage.getSize())
                .totalElements(dtoPage.getTotalElements())
                .totalPages(dtoPage.getTotalPages())
                .first(dtoPage.isFirst())
                .last(dtoPage.isLast())
                .build();

        logger.info("[{}] [{}] Listagem de configurações concluída. Total de elementos: {}, Total de páginas: {}",
                serviceName, operation, dtoPage.getTotalElements(), dtoPage.getTotalPages());

        String message = buildListMessage(pageableDto, "configuração", "configurações");

        return ResponseDto.<PageableDto<DataConfigResponseDto>>builder()
                .data(pageableDto)
                .message(message)
                .success(true)
                .statusCode(200)
                .build();
    }

    /**
     * Atualiza uma configuração existente.
     */
    @Transactional
    public ResponseDto<DataConfigResponseDto> updateDataConfig(String namespace, String environment, String key, String newValue) {
        String serviceName = "ConfigService";
        String operation = "UPDATE_DATA_CONFIG";

        logger.info("[{}] [{}] Iniciando atualização de configuração. Namespace: {}, Environment: {}, Key: {}",
                serviceName, operation, namespace, environment, key);


        DataEntity dataEntity = dataRepository
                .findByKeyAndEnvironmentNamespaceNameAndEnvironmentName(key, namespace, environment)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Configuração '%s' não encontrada para namespace '%s' e ambiente '%s'",
                                key, namespace, environment)));

        String oldValue = dataEntity.getValue();

        if (oldValue != null && oldValue.equals(newValue)) {
            logger.warn("[{}] [{}] Nenhum dado para atualizar. Key: {}, Valor atual já é: {}",
                    serviceName, operation, key, newValue);
            return ResponseDto.<DataConfigResponseDto>builder()
                    .data(toResponseDto(dataEntity))
                    .message("Configuração já possui o valor informado")
                    .success(true)
                    .statusCode(304)
                    .build();
        }

        String cacheKey = "CONFIG_CACHE::config_" + dataEntity.getId() + "_" + key;

        dataEntity.setValue(newValue);
        dataRepository.save(dataEntity);

        logger.info("[{}] [{}] Persistindo histórico da configuração atualizada. ID: {}, Key: {}",
                serviceName, operation, dataEntity.getId(), key);

        saveHistoryConfig(dataEntity, OperationType.UPDATE, oldValue, newValue, null, key);

        DataConfigResponseDto responseDto = toResponseDto(dataEntity);

        customCacheManager.saveToCache(cacheKey, responseDto);
        logger.info("[{}] [{}] Cache atualizado: {}", serviceName, operation, cacheKey);

        logger.info("[{}] [{}] Configuração atualizada com sucesso. ID: {}, Key: {}, Old Value: {}, New Value: {}",
                serviceName, operation, dataEntity.getId(), key, oldValue, newValue);

        sendConfigEventToKafka(dataEntity, "UPDATE", serviceName, operation);

        return ResponseDto.<DataConfigResponseDto>builder()
                .data(responseDto)
                .message(String.format("Configuração '%s' atualizada com sucesso", key))
                .success(true)
                .statusCode(200)
                .build();
    }

    /**
     * Busca uma configuração específica por namespace, environment e key
     */
    public ResponseDto<DataConfigResponseDto> getDataConfig(String namespace, String environment, String key) {
        String serviceName = "ConfigService";
        String operation = "GET_DATA_CONFIG";

        logger.info("[{}] [{}] Buscando configuração. Namespace: {}, Environment: {}, Key: {}",
                serviceName, operation, namespace, environment, key);

        DataEntity dataEntity = dataRepository
                .findByKeyAndEnvironmentNamespaceNameAndEnvironmentName(key, namespace, environment)
                .orElseThrow(() -> {
                    logger.error("[{}] [{}] Configuração não encontrada - Namespace: {}, Environment: {}, Key: {}",
                            serviceName, operation, namespace, environment, key);
                    return new EntityNotFoundException(
                            String.format("Configuração '%s' não encontrada para namespace '%s' e ambiente '%s'",
                                    key, namespace, environment));
                });

        String cachePattern = "CONFIG_CACHE::config_" + dataEntity.getId() + "_*";
        DataConfigResponseDto cached = customCacheManager.getFromCache(cachePattern, DataConfigResponseDto.class);

        if (cached != null) {
            logger.info("[{}] [{}] Cache HIT para Key: {}", serviceName, operation, key);
            return buildSuccessResponse(cached, "Dados do cache");
        }

        logger.info("[{}] [{}] Cache MISS. Dados obtidos do banco. Key: {}", serviceName, operation, key);

        DataConfigResponseDto responseDto = toResponseDto(dataEntity);


        String cacheKey = "CONFIG_CACHE::config_" + dataEntity.getId() + "_" + key;
        customCacheManager.saveToCache(cacheKey, responseDto);

        logger.info("[{}] [{}] Configuração encontrada e cached. Key: {}, ID: {}",
                serviceName, operation, key, dataEntity.getId());

        return buildSuccessResponse(responseDto,
                String.format("Configuração '%s' encontrada com sucesso", key));
    }

    /**
     * Deleta uma configuração específica por namespace, environment e key
     */
    @Transactional
    public ResponseDto<DataConfigResponseDto> deleteDataConfig(String namespace, String environment, String key) {
        String serviceName = "ConfigService";
        String operation = "DELETE_DATA_CONFIG";

        logger.info("[{}] [{}] Iniciando exclusão de configuração. Namespace: {}, Environment: {}, Key: {}",
                serviceName, operation, namespace, environment, key);

        DataEntity dataEntity = dataRepository
                .findByKeyAndEnvironmentNamespaceNameAndEnvironmentName(key, namespace, environment)
                .orElseThrow(() -> {
                    logger.error("[{}] [{}] Configuração não encontrada para exclusão - Namespace: {}, Environment: {}, Key: {}",
                            serviceName, operation, namespace, environment, key);
                    return new EntityNotFoundException(
                            String.format("Configuração '%s' não encontrada para namespace '%s' e ambiente '%s'",
                                    key, namespace, environment));
                });


        DataConfigResponseDto responseDto = toResponseDto(dataEntity);
        String deletedValue = dataEntity.getValue();
        UUID dataId = dataEntity.getId();

        String cachePattern = "CONFIG_CACHE::config_" + dataId + "_*";
        customCacheManager.deleteFromCache(cachePattern);
        logger.info("[{}] [{}] Cache invalidado para padrão: {}", serviceName, operation, cachePattern);

        logger.info("[{}] [{}] Persistindo histórico da configuração excluída. ID: {}, Key: {}",
                serviceName, operation, dataId, key);
        saveHistoryConfig(dataEntity, OperationType.DELETE, deletedValue, null, key, null);

        dataRepository.delete(dataEntity);

        sendConfigEventToKafka(dataEntity, "DELETE", serviceName, operation);

        logger.info("[{}] [{}] Configuração excluída com sucesso. Namespace: {}, Environment: {}, Key: {}, ID: {}",
                serviceName, operation, namespace, environment, key, dataId);

        return ResponseDto.<DataConfigResponseDto>builder()
                .data(responseDto)
                .message(String.format("Configuração '%s' excluída com sucesso", key))
                .success(true)
                .statusCode(200)
                .build();
    }

    /**
     * Busca o histórico de configurações por namespace e ambiente com paginação.
     */
    @Transactional(readOnly = true)
    public ResponseDto<PageableDto<HistoryConfigResponseDto>> getHistory(String namespace, String environment, Pageable pageable) {
        String serviceName = "ConfigService";
        String operation = "GET_HISTORY_BY_ENVIRONMENT";

        logger.info("[{}] [{}] Iniciando busca de histórico. Namespace: {}, Environment: {}, Página: {}, Tamanho: {}",
                serviceName, operation, namespace, environment, pageable.getPageNumber(), pageable.getPageSize());

        EnvironmentEntity environmentEntity = environmentRepository
                .findByNamespaceNameAndName(namespace, environment)
                .orElseThrow(() -> {
                    logger.error("[{}] [{}] Ambiente não encontrado - Namespace: {}, Environment: {}",
                            serviceName, operation, namespace, environment);
                    return new EntityNotFoundException(
                            String.format("Ambiente '%s' não encontrado para namespace '%s'", environment, namespace));
                });

        UUID environmentId = environmentEntity.getId();
        logger.debug("[{}] [{}] Environment encontrado. ID: {}", serviceName, operation, environmentId);

        Page<HistoryConfigEntity> historyEntities = fetchHistoryEntities(environmentId, pageable, serviceName, operation);

        List<HistoryConfigResponseDto> historyDtos = mapToDto(historyEntities);

        PageableDto<HistoryConfigResponseDto> pageableDto = buildPageableDto(historyEntities, historyDtos);

        logger.info("[{}] [{}] Histórico encontrado. Namespace: {}, Environment: {}, Total: {}, Páginas: {}",
                serviceName, operation, namespace, environment, historyEntities.getTotalElements(), historyEntities.getTotalPages());

        return ResponseDto.<PageableDto<HistoryConfigResponseDto>>builder()
                .data(pageableDto)
                .message(String.format("Histórico encontrado para namespace '%s' e ambiente '%s'", namespace, environment))
                .success(true)
                .statusCode(200)
                .build();
    }

    // => Auxilia a buildar o retorno de sucesso
    private ResponseDto<DataConfigResponseDto> buildSuccessResponse(DataConfigResponseDto data, String message) {
        return ResponseDto.<DataConfigResponseDto>builder()
                .data(data)
                .message(message)
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
            return "Nenhuma " + singular + " encontrada. Página " + currentPage +
                    " de " + totalPages + ", Tamanho da página: " + pageSize;
        } else if (totalElements == 1) {
            return "1 " + singular + " encontrada. Página " + currentPage +
                    " de " + totalPages + ", Tamanho da página: " + pageSize;
        } else {
            return totalElements + " " + plural + " encontradas. Página " + currentPage +
                    " de " + totalPages + ", Tamanho da página: " + pageSize;
        }
    }

    // => Auxilia encontrar o ambiente
    private EnvironmentEntity findEnvironment(UUID environmentId, String serviceName, String operation) {
        logger.debug("[{}] [{}] Buscando ambiente no repositório. Ambiente ID: {}", serviceName, operation, environmentId);
        return environmentRepository.findById(environmentId)
                .orElseThrow(() -> new EntityNotFoundException("Ambiente não encontrado"));
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
