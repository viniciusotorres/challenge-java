package com.example.config_service_api.service;

import com.example.config_service_api.dto.*;
import com.example.config_service_api.entity.ConfigurationEntity;
import com.example.config_service_api.entity.Environment;
import com.example.config_service_api.entity.Namespace;
import com.example.config_service_api.repository.ConfigurationRepository;
import com.example.config_service_api.repository.EnvironmentRepository;
import com.example.config_service_api.repository.NamespaceRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationService.class);

    private final ConfigurationRepository configurationRepository;
    private final EnvironmentRepository environmentRepository;
    private final NamespaceRepository namespaceRepository;

    public ConfigurationService(ConfigurationRepository configurationRepository, EnvironmentRepository environmentRepository, NamespaceRepository namespaceRepository) {
        this.configurationRepository = configurationRepository;
        this.environmentRepository = environmentRepository;
        this.namespaceRepository = namespaceRepository;
    }

    /**
     * Cria uma nova configuração.
     *
     * @param config DTO contendo os dados da configuração a ser criada.
     * @return ResponseCreateDto com a configuração criada e mensagem de sucesso.
     */
    public ResponseDto<ConfigurationDto> createConfiguration(CreateConfigurationDto config) {
        validateConfigurationDoesNotExist(config);

        Environment environment = findOrCreateEnvironment(config.environment().name());
        Namespace namespace = findOrCreateNamespace(config.namespace().name());

        ConfigurationEntity configurationEntity = buildConfigurationEntity(config, environment, namespace);

        ConfigurationEntity savedEntity = configurationRepository.save(configurationEntity);

        ConfigurationDto dto = mapToDto(savedEntity);

        return new ResponseDto<>(dto, "Configuração criada com sucesso", 201);
    }

    /**
     * Atualiza uma configuração existente.
     *
     * @param id     ID da configuração a ser atualizada.
     * @param config DTO contendo os novos dados da configuração.
     * @return ResponseCreateDto com a configuração atualizada e mensagem de sucesso.
     */
    public ResponseDto updateConfiguration(UUID id, UpdateConfigurationDto config) {
        ConfigurationEntity existingConfig = configurationRepository.findById(id).orElseThrow(() -> new EntityExistsException("Configuração não encontrada com o ID: " + id));


        Environment environment = findOrCreateEnvironment(config.environment().name());
        Namespace namespace = findOrCreateNamespace(config.namespace().name());

        existingConfig.setNamespace(namespace);
        existingConfig.setEnvironment(environment);
        existingConfig.setKey(config.key());
        existingConfig.setValue(config.value());
        existingConfig.setUpdatedAt(LocalDateTime.now());

        configurationRepository.save(existingConfig);

        return new ResponseDto<>(existingConfig, "Configuração atualizada com sucesso", 200);
    }

    /**
     * Obtém uma configuração pelo ID.
     *
     * @param id ID da configuração a ser obtida.
     * @return ConfigurationDto com os dados da configuração.
     */
    public ConfigurationDto getConfiguration(UUID id) {
        logger.info("Buscando configuração com ID: {}", id);

        ConfigurationEntity configurationEntity = configurationRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Configuração não encontrada com o ID: " + id));

        return mapToDto(configurationEntity);
    }

    /**
     * Deleta uma configuração pelo ID.
     *
     * @param id ID da configuração a ser deletada.
     */
    public void deleteConfiguration(UUID id) {
        logger.info("Deletando configuração com ID: {}", id);

        ConfigurationEntity configurationEntity = configurationRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Configuração não encontrada com o ID: " + id));

        configurationRepository.delete(configurationEntity);
        logger.info("Configuração com ID {} deletada com sucesso", id);
    }

    /**
     * Mapeia uma entidade de configuração para um DTO.
     *
     * @param configurationEntity A entidade de configuração a ser mapeada.
     * @return ConfigurationDto com os dados da configuração.
     */
    private ConfigurationDto mapToDto(ConfigurationEntity configurationEntity) {
        return new ConfigurationDto(configurationEntity.getId(), configurationEntity.getKey(), configurationEntity.getValue(), new EnvironmentDto(configurationEntity.getEnvironment().getId(), configurationEntity.getEnvironment().getName()), new NamespaceDto(configurationEntity.getNamespace().getId(), configurationEntity.getNamespace().getName()), configurationEntity.getCreatedAt(), configurationEntity.getUpdatedAt());
    }

    /**
     * Valida se a configuração já existe com a mesma chave e valor.
     *
     * @param config DTO contendo os dados da configuração a ser validada.
     * @throws EntityExistsException se a configuração já existir.
     */
    private void validateConfigurationDoesNotExist(CreateConfigurationDto config) {
        if (configurationRepository.existsByKeyAndValue(config.key(), config.value())) {
            logger.error("Configuração já existe com a chave e valor fornecidos: {}", config);
            throw new EntityExistsException("Configuração já existe com a chave e valor fornecidos.");
        }
    }

    /**
     * Encontra ou cria um ambiente com o nome fornecido.
     *
     * @param environmentName Nome do ambiente a ser encontrado ou criado.
     * @return Environment encontrado ou criado.
     */
    private Environment findOrCreateEnvironment(String environmentName) {
        return environmentRepository.findByName(environmentName).orElseGet(() -> {
            logger.info("Criando environment: {}", environmentName);
            Environment newEnv = new Environment();
            newEnv.setName(environmentName);
            return environmentRepository.save(newEnv);
        });
    }

    /**
     * Encontra ou cria um namespace com o nome fornecido.
     *
     * @param namespaceName Nome do namespace a ser encontrado ou criado.
     * @return Namespace encontrado ou criado.
     */
    private Namespace findOrCreateNamespace(String namespaceName) {
        return namespaceRepository.findByName(namespaceName).orElseGet(() -> {
            logger.info("Criando namespace: {}", namespaceName);
            Namespace newNamespace = new Namespace();
            newNamespace.setName(namespaceName);
            return namespaceRepository.save(newNamespace);
        });
    }

    /**
     * Constrói uma entidade de configuração a partir do DTO fornecido.
     *
     * @param config      DTO contendo os dados da configuração.
     * @param environment Ambiente associado à configuração.
     * @param namespace   Namespace associado à configuração.
     * @return ConfigurationEntity com os dados da configuração.
     */
    private ConfigurationEntity buildConfigurationEntity(CreateConfigurationDto config, Environment environment, Namespace namespace) {
        logger.info("Criando configuração: {}", config);
        ConfigurationEntity configurationEntity = new ConfigurationEntity();
        configurationEntity.setNamespace(namespace);
        configurationEntity.setEnvironment(environment);
        configurationEntity.setKey(config.key());
        configurationEntity.setValue(config.value());
        configurationEntity.setCreatedAt(LocalDateTime.now());
        configurationEntity.setUpdatedAt(LocalDateTime.now());
        return configurationEntity;
    }


}
