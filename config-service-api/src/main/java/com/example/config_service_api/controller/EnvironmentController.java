package com.example.config_service_api.controller;

import com.example.config_service_api.dto.*;
import com.example.config_service_api.service.EnvironmentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/environment")

public class EnvironmentController {

    private Logger logger = LoggerFactory.getLogger(EnvironmentController.class);

    private final EnvironmentService environmentService;

    public EnvironmentController(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @PostMapping("/create")
    public ResponseEntity<ResponseDto> createEnvironment(@Valid @RequestBody EnvironmentCreateDto environmentCreateDto) {
        logger.info("Requisição recebida [POST /environment/create] - Corpo da requisição: {}", environmentCreateDto);
        ResponseDto response = environmentService.createEnvironment(environmentCreateDto);
        return respondWithLogging(response, response.statusCode(), response.message());
    }

    @GetMapping("/list")
    public ResponseEntity<ResponseDto<PageableDto<EnvironmentResponseDto>>>listEnvironments(Pageable pageable) {
        logger.info("Requisição recebida [GET /environment/list]");
        ResponseDto<PageableDto<EnvironmentResponseDto>> response = environmentService.listEnvironments(pageable);
        return respondWithLogging(response, response.statusCode(), response.message());
    }


    @PutMapping("/update/{id}")
    public ResponseEntity<ResponseDto> updateEnvironment(@PathVariable UUID id, @Valid @RequestBody EnvironmentUpdateDto environmentUpdateDto) {
        logger.info("Requisição recebida [PUT /environment/update/{}] - Corpo da requisição: {}", id, environmentUpdateDto);
        ResponseDto response = environmentService.updateEnvironment(id, environmentUpdateDto);
        return respondWithLogging(response, response.statusCode(), response.message());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDto> getEnvironmentById(@PathVariable UUID id) {
        logger.info("Requisição recebida [GET /environment/{}]", id);
        ResponseDto response = environmentService.getEnvironmentById(id);
        return respondWithLogging(response, response.statusCode(), response.message());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDto> deleteEnvironment(@PathVariable UUID id) {
        logger.info("Requisição recebida [DELETE /environment/{}]", id);
        ResponseDto response = environmentService.deleteEnvironment(id);
        return respondWithLogging(response, response.statusCode(), response.message());
    }

    private <T> ResponseEntity<T> respondWithLogging(T body, int statusCode, String message) {
        logger.info("Resposta enviada [status: {}] - Mensagem: {}", statusCode, message);
        return ResponseEntity.status(statusCode).body(body);
    }







}
