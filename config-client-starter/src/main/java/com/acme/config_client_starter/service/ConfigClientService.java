package com.acme.config_client_starter.service;


import com.acme.config_client_starter.dto.ConfigRequestDto;
import com.acme.config_client_starter.dto.ConfigResponseWrapper;
import com.acme.config_client_starter.dto.DataConfigResponseDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigClientService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigClientService.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;


    public ConfigClientService(RestTemplate restTemplate) {
        this(restTemplate, "http://localhost:8080");
    }

    public ConfigClientService(RestTemplate restTemplate, String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public DataConfigResponseDto getConfig(String key) {
        try {
            String url = baseUrl + "/data-config/" + key;
            logger.debug("Buscando configuração para key: {}", key);
            ConfigResponseWrapper<DataConfigResponseDto> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<ConfigResponseWrapper<DataConfigResponseDto>>() {}
                    ).getBody();

            return response != null ? response.data() : null;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) return null;
            throw e;
        }
    }

    public boolean createConfig(String key, String value) {
        String url = baseUrl + "/data-config/create";
        ConfigRequestDto request = new ConfigRequestDto(key, value);
        ResponseEntity<Void> response = restTemplate.postForEntity(url, request, Void.class);
        return response.getStatusCode().is2xxSuccessful();
    }

    public boolean updateConfig(String id, String value) {
        String url = baseUrl + "/data-config/update/" + id;
        ConfigRequestDto request = new ConfigRequestDto(null, value);
        restTemplate.put(url, request);
        return true;
    }

    public boolean deleteConfig(String id) {
        String url = baseUrl + "/data-config/" + id;
        restTemplate.delete(url);
        return true;
    }

    public String getAllConfigs() {
        String url = baseUrl + "/data-config/list";
        try {
            logger.debug("Buscando todas as configurações");
            return restTemplate.getForObject(url, String.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) return null;
            throw e;
        }
    }

    public String getConfigsByEnvironment(String environmentId) {
        String url = baseUrl + "/data-config/list-by-environment/" + environmentId;
        try {
            logger.debug("Buscando configurações para o ambiente: {}", environmentId);
            return restTemplate.getForObject(url, String.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) return null;
            throw e;
        }
    }


}