package com.example.config_service_api.controller;

import com.example.config_service_api.dto.EnvironmentCreateDto;
import com.example.config_service_api.dto.EnvironmentUpdateDto;
import com.example.config_service_api.dto.PageableDto;
import com.example.config_service_api.dto.ResponseDto;
import com.example.config_service_api.service.EnvironmentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
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
        logger.info("Resposta enviada [status: {}] - Mensagem: {}", response.statusCode(), response.message());
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @GetMapping("/list")
    public ResponseEntity<PageableDto> listEnvironments(Pageable pageable) {
        logger.info("Requisição recebida [GET /environment/list]");
        ResponseDto response = environmentService.listEnvironments(pageable);
        logger.info("Resposta enviada [status: {}] - Mensagem: {}", response.statusCode(), response.message());
        return ResponseEntity.status(response.statusCode()).body((PageableDto) response.data());
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ResponseDto> updateEnvironment(@PathVariable UUID id, @Valid @RequestBody EnvironmentUpdateDto environmentUpdateDto) {
        logger.info("Requisição recebida [PUT /environment/update/{}] - Corpo da requisição: {}", id, environmentUpdateDto);
        ResponseDto response = environmentService.updateEnvironment(id, environmentUpdateDto);
        logger.info("Resposta enviada [status: {}] - Mensagem: {}", response.statusCode(), response.message());
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDto> getEnvironmentById(@PathVariable UUID id) {
        logger.info("Requisição recebida [GET /environment/{}]", id);
        ResponseDto response = environmentService.getEnvironmentById(id);
        logger.info("Resposta enviada [status: {}] - Mensagem: {}", response.statusCode(), response.message());
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDto> deleteEnvironment(@PathVariable UUID id) {
        logger.info("Requisição recebida [DELETE /environment/{}]", id);
        ResponseDto response = environmentService.deleteEnvironment(id);
        logger.info("Resposta enviada [status: {}] - Mensagem: {}", response.statusCode(), response.message());
        return ResponseEntity.status(response.statusCode()).body(response);
    }






}
