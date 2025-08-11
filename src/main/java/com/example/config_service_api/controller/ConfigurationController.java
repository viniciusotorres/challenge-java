package com.example.config_service_api.controller;

import com.example.config_service_api.dto.*;
import com.example.config_service_api.service.ConfigurationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/api/configurations")
public class ConfigurationController {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationController.class);

    private final ConfigurationService configurationService;

    public ConfigurationController(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @PostMapping("/create")
    @Transactional
    public ResponseEntity<ResponseDto<ConfigurationDto>> createConfiguration(@Valid @RequestBody CreateConfigurationDto config, UriComponentsBuilder uriBuilder) {
        logger.info("Recebida requisição para criar configuração: {}", config);
        ResponseDto<ConfigurationDto> response = configurationService.createConfiguration(config);

        UUID id = response.data().id();
        var uri = uriBuilder.path("/api/configurations/get/{id}")
                .buildAndExpand(id)
                .toUri();

        logger.info("Configuração criada com sucesso: {}", response);
        return ResponseEntity.created(uri).body(response);
    }

    @PutMapping("/update/{id}")
    @Transactional
    public ResponseEntity<ResponseDto<ConfigurationDto>> updateConfiguration(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateConfigurationDto config) {
        logger.info("Recebida requisição para atualizar configuração com ID {}: {}", id, config);
        ResponseDto<ConfigurationDto> response = configurationService.updateConfiguration(id, config);
        logger.info("Configuração atualizada com sucesso: {}", response);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/get/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ConfigurationDto> getConfiguration(@PathVariable UUID id) {
        logger.info("Recebida requisição para obter configuração com ID {}", id);
        ConfigurationDto response = configurationService.getConfiguration(id);
        logger.info("Configuração obtida com sucesso: {}", response);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete/{id}")
    @Transactional
    public ResponseEntity<Void> deleteConfiguration(@PathVariable UUID id) {
        logger.info("Recebida requisição para deletar configuração com ID {}", id);
        configurationService.deleteConfiguration(id);
        logger.info("Configuração com ID {} deletada com sucesso", id);
        return ResponseEntity.noContent().build();
    }


}
