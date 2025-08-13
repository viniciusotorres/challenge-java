package com.example.config_service_api.controller;

import com.example.config_service_api.dto.NamespaceCreateDto;
import com.example.config_service_api.dto.NamespaceUpdateDto;
import com.example.config_service_api.dto.PageableDto;
import com.example.config_service_api.dto.ResponseDto;
import com.example.config_service_api.service.NamespaceService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/namespace")
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
        logger.info("Resposta enviada [status: {}] - Mensagem: {}", response.statusCode(), response.message());
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @GetMapping("/list")
    public ResponseEntity<PageableDto> listNamespaces(Pageable pageable) {
        logger.info("Requisição recebida [GET /namespaces/list]");
        ResponseDto response = namespaceService.listNamespaces(pageable);
        logger.info("Resposta enviada [status: {}] - Mensagem: {}", response.statusCode(), response.message());
        return ResponseEntity.status(response.statusCode()).body((PageableDto) response.data());
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ResponseDto> updateNamespace(@PathVariable UUID id, @Valid @RequestBody NamespaceUpdateDto namespaceUpdateDto) {
        logger.info("Requisição recebida [PUT /namespaces/update/{}] - Corpo da requisição: {}", id, namespaceUpdateDto);
        ResponseDto response = namespaceService.updateNamespace(id, namespaceUpdateDto);
        logger.info("Resposta enviada [status: {}] - Mensagem: {}", response.statusCode(), response.message());
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDto> getNamespaceById(@PathVariable UUID id) {
        logger.info("Requisição recebida [GET /namespaces/{}]", id);
        ResponseDto response = namespaceService.getNamespaceById(id);
        logger.info("Resposta enviada [status: {}] - Mensagem: {}", response.statusCode(), response.message());
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDto> deleteNamespace(@PathVariable UUID id) {
        logger.info("Requisição recebida [DELETE /namespaces/{}]", id);
        ResponseDto response = namespaceService.deleteNamespace(id);
        logger.info("Resposta enviada [status: {}] - Mensagem: {}", response.statusCode(), response.message());
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @GetMapping("/{id}/environments")
    public ResponseEntity<ResponseDto> getEnvironmentsByNamespaceId(@PathVariable UUID id, Pageable pageable) {
        logger.info("Requisição recebida [GET /namespaces/{}/ environments]", id);
        ResponseDto response = namespaceService.getEnvironmentsByNamespaceId(id, pageable);
        logger.info("Resposta enviada [status: {}] - Mensagem: {}", response.statusCode(), response.message());
        return ResponseEntity.status(response.statusCode()).body(response);
    }

}
