package com.acme.config_client_starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "acme.config-client")
public class ConfigClientProperties {
    private long connectionTimeout = 5000;
    private long readTimeout = 5000;
    private String baseUrl = "http://localhost:8080";
    private String kafkaBootstrapServers = "localhost:9092";
    private String kafkaTopic = "config-topic";

    public long getConnectionTimeout() {
        return connectionTimeout;
    }
    public void setConnectionTimeout(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    public long getReadTimeout() {
        return readTimeout;
    }
    public void setReadTimeout(long readTimeout) {
        this.readTimeout = readTimeout;
    }
    public String getBaseUrl() {
        return baseUrl;
    }
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getKafkaBootstrapServers() {
        return kafkaBootstrapServers;
    }

    public void setKafkaBootstrapServers(String kafkaBootstrapServers) {
        this.kafkaBootstrapServers = kafkaBootstrapServers;
    }

    public String getKafkaTopic() {
        return kafkaTopic;
    }

    public void setKafkaTopic(String kafkaTopic) {
        this.kafkaTopic = kafkaTopic;
    }
}