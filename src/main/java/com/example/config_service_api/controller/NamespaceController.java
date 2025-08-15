package com.example.config_service_api.controller;

import com.example.config_service_api.dto.*;
import com.example.config_service_api.service.NamespaceService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/namespaces")
public class NamespaceController {

    private Logger logger = LoggerFactory.getLogger(NamespaceController.class);

    private final NamespaceService namespaceService;

    public NamespaceController(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    @PostMapping("/create")
    public ResponseEntity<ResponseDto> createNamespace(@Valid @RequestBody NamespaceCreateDto namespaceCreateDto) {
        logger.info("Requisição recebida [POST /namespaces/create] - Corpo da requisição: {}", namespaceCreateDto);
        ResponseDto response = namespaceService.createNamespace(namespaceCreateDto);
        return respondWithLogging(response, response.statusCode(), response.message());
    }

    @GetMapping("/list")
    public ResponseEntity<ResponseDto<PageableDto<NamespaceResponseDto>>> listNamespaces(Pageable pageable) {
        logger.info("Requisição recebida [GET /namespaces/list]");
        ResponseDto<PageableDto<NamespaceResponseDto>> response = namespaceService.listNamespaces(pageable);
        return respondWithLogging(response, response.statusCode(), response.message());
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ResponseDto> updateNamespace(@PathVariable UUID id, @Valid @RequestBody NamespaceUpdateDto namespaceUpdateDto) {
        logger.info("Requisição recebida [PUT /namespaces/update/{}] - Corpo da requisição: {}", id, namespaceUpdateDto);
        ResponseDto response = namespaceService.updateNamespace(id, namespaceUpdateDto);
        return respondWithLogging(response, response.statusCode(), response.message());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDto> getNamespaceById(@PathVariable UUID id) {
        logger.info("Requisição recebida [GET /namespaces/{}]", id);
        ResponseDto response = namespaceService.getNamespaceById(id);
        return respondWithLogging(response, response.statusCode(), response.message());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDto> deleteNamespace(@PathVariable UUID id) {
        logger.info("Requisição recebida [DELETE /namespaces/{}]", id);
        ResponseDto response = namespaceService.deleteNamespace(id);
        return respondWithLogging(response, response.statusCode(), response.message());
    }

    @GetMapping("/{id}/environments")
    public ResponseEntity<ResponseDto> getEnvironmentsByNamespaceId(@PathVariable UUID id, Pageable pageable) {
        logger.info("Requisição recebida [GET /namespaces/{}/ environments]", id);
        ResponseDto response = namespaceService.getEnvironmentsByNamespaceId(id, pageable);
        return respondWithLogging(response, response.statusCode(), response.message());
    }

    private <T> ResponseEntity<T> respondWithLogging(T body, int statusCode, String message) {
        logger.info("Resposta enviada [status: {}] - Mensagem: {}", statusCode, message);
        return ResponseEntity.status(statusCode).body(body);
    }

}
