package com.example.config_service_api.exception;

import com.example.config_service_api.dto.ResponseDto;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseDto<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        logger.warn("Erro de validação: {}", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseDto.<Map<String, String>>builder()
                        .data(errors)
                        .message("Erro de validação")
                        .success(false)
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .build());
    }


    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ResponseDto<Void>> handleEntityNotFound(EntityNotFoundException ex) {
        logger.warn("Recurso não encontrado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseDto.<Void>builder()
                        .message(ex.getMessage() != null ? ex.getMessage() : "Recurso não encontrado")
                        .success(false)
                        .statusCode(HttpStatus.NOT_FOUND.value())
                        .build());
    }


    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ResponseDto<Void>> handleConflict(DataIntegrityViolationException ex) {
        logger.warn("Conflito de dados: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ResponseDto.<Void>builder()
                        .message(ex.getMessage())
                        .success(false)
                        .statusCode(HttpStatus.CONFLICT.value())
                        .build());
    }



    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDto<Void>> handleGenericException(Exception ex) {
        logger.error("Erro inesperado: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseDto.<Void>builder()
                        .message("Ocorreu um erro inesperado")
                        .success(false)
                        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .build());
    }
}