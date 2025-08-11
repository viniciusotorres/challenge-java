package com.example.config_service_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ConfigServiceApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConfigServiceApiApplication.class, args);
	}

}
