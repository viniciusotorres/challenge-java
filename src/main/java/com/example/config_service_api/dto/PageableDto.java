package com.example.config_service_api.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record PageableDto<T>(
        List<T> content,
        int currentPage,
        int pageSize,
        Long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {}
