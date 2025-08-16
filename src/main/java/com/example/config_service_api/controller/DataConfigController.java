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

    @GetMapping("/list")
    public ResponseEntity<ResponseDto<PageableDto<DataConfigResponseDto>>> listDataConfigs(Pageable pageable) {
        logger.info("Requisição recebida [GET /data-config/list]");
        ResponseDto<PageableDto<DataConfigResponseDto>> response = dataConfigService.listDataConfigs(pageable);
        return respondWithLogging(response, response.statusCode(), response.message());
    }

    @GetMapping("/list-by-environment/{environmentId}")
    public ResponseEntity<ResponseDto<PageableDto<DataConfigResponseDto>>> listDataConfigsByEnvironment(
            @PathVariable UUID environmentId, Pageable pageable) {
        logger.info("Requisição recebida [GET /data-config/list-by-environment/{}]", environmentId);
        ResponseDto<PageableDto<DataConfigResponseDto>> response = dataConfigService.listDataConfigsByEnvironment(environmentId, pageable);
        return respondWithLogging(response, response.statusCode(), response.message());
    }

    @PutMapping("/update/{id}")
    @Transactional
    public ResponseEntity<ResponseDto> updateDataConfig(@PathVariable UUID id, @Valid @RequestBody DataConfigUpdateDto dataConfigUpdateDto) {
        logger.info("Requisição recebida [PUT /data-config/update/{}] - Corpo da requisição: {}", id, dataConfigUpdateDto);
        ResponseDto response = dataConfigService.updateDataConfig(id, dataConfigUpdateDto);
        return respondWithLogging(response, response.statusCode(), response.message());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDto> getDataConfigById(@PathVariable UUID id) {
        logger.info("Requisição recebida [GET /data-config/{}]", id);
        ResponseDto response = dataConfigService.getDataConfigById(id);
        return respondWithLogging(response, response.statusCode(), response.message());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDto> deleteDataConfig(@PathVariable UUID id) {
        logger.info("Requisição recebida [DELETE /data-config/{}]", id);
        ResponseDto response = dataConfigService.deleteDataConfig(id);
        return respondWithLogging(response, response.statusCode(), response.message());
    }

    @GetMapping("/history-by-environment/{environmentId}")
    public ResponseEntity<ResponseDto<PageableDto<HistoryConfigResponseDto>>> getHistoryByEnvironment(
            @PathVariable UUID environmentId, Pageable pageable) {
        logger.info("Requisição recebida [GET /data-config/history-by-environment/{}]", environmentId);

        ResponseDto<PageableDto<HistoryConfigResponseDto>> response = dataConfigService.getHistoryByEnvironment(environmentId, pageable);

        return respondWithLogging(response, response.statusCode(), response.message());
    }


    private <T> ResponseEntity<T> respondWithLogging(T body, int statusCode, String message) {
        logger.info("Resposta enviada [status: {}] - Mensagem: {}", statusCode, message);
        return ResponseEntity.status(statusCode).body(body);
    }
}
