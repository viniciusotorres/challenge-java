package com.example.config_service_api.controller;

import com.example.config_service_api.dto.*;
import com.example.config_service_api.service.DataConfigService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/data-config")
public class DataConfigController {

    private static final Logger logger = LoggerFactory.getLogger(DataConfigController.class);

    private final DataConfigService dataConfigService;

    public DataConfigController(DataConfigService dataConfigService) {
        this.dataConfigService = dataConfigService;
    }

    @PostMapping("/create")
    @Transactional
    public ResponseEntity<ResponseDto> createDataConfig(@Valid @RequestBody DataConfigCreateDto dataConfigCreateDto) {
        logger.info("Requisição recebida [POST /data-config/create] - Corpo da requisição: {}", dataConfigCreateDto);
        ResponseDto response = dataConfigService.createDataConfig(dataConfigCreateDto);
        return respondWithLogging(response, response.statusCode(), response.message());
    }

    @GetMapping("/{namespace}/{environment}/configs")
    public ResponseEntity<ResponseDto<PageableDto<DataConfigResponseDto>>> listConfigs(
            @PathVariable String namespace,
            @PathVariable String environment,
            Pageable pageable) {

        logger.info("Requisição recebida [GET /data-config/{}/{}/configs", namespace, environment);

        ResponseDto<PageableDto<DataConfigResponseDto>> response =
                dataConfigService.listConfigsByNamespaceAndEnv(namespace, environment, pageable);

        return respondWithLogging(response, response.statusCode(), response.message());
    }


    @PutMapping("/{namespace}/{environment}/configs/{key}")
    @Transactional
    public ResponseEntity<ResponseDto> updateDataConfig(
            @PathVariable String namespace,
            @PathVariable String environment,
            @PathVariable String key,
            @RequestBody String value) {

        logger.info("Requisição recebida [PUT /data-config/{}/{}/configs/{}] - Novo valor: {}",
                namespace, environment, key, value);

        ResponseDto response = dataConfigService.updateDataConfig(namespace, environment, key, value);
        return respondWithLogging(response, response.statusCode(), response.message());
    }


    @GetMapping("/{namespace}/{environment}/configs/{key}")
    public ResponseEntity<ResponseDto> getDataConfig(
            @PathVariable String namespace,
            @PathVariable String environment,
            @PathVariable String key) {

        logger.info("Requisição recebida [GET /data-config/{}/{}/configs/{}]",
                namespace, environment, key);

        ResponseDto response = dataConfigService.getDataConfig(namespace, environment, key);
        return respondWithLogging(response, response.statusCode(), response.message());
    }

    @DeleteMapping("/{namespace}/{environment}/configs/{key}")
    @Transactional
    public ResponseEntity<ResponseDto> deleteDataConfig(
            @PathVariable String namespace,
            @PathVariable String environment,
            @PathVariable String key) {

        logger.info("Requisição recebida [DELETE /data-config/{}/{}/configs/{}]",
                namespace, environment, key);

        ResponseDto response = dataConfigService.deleteDataConfig(namespace, environment, key);
        return respondWithLogging(response, response.statusCode(), response.message());
    }

    @GetMapping("/{namespace}/{environment}/history")
    public ResponseEntity<ResponseDto<PageableDto<HistoryConfigResponseDto>>> getHistory(
            @PathVariable String namespace,
            @PathVariable String environment,
            Pageable pageable) {

        logger.info("Requisição recebida [GET /data-config/{}/{}/history]",
                namespace, environment);

        ResponseDto<PageableDto<HistoryConfigResponseDto>> response =
                dataConfigService.getHistory(namespace, environment, pageable);

        return respondWithLogging(response, response.statusCode(), response.message());
    }

    private <T> ResponseEntity<T> respondWithLogging(T body, int statusCode, String message) {
        logger.info("Resposta enviada [status: {}] - Mensagem: {}", statusCode, message);
        return ResponseEntity.status(statusCode).body(body);
    }
}
