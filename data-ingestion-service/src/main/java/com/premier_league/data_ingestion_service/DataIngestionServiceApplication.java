package com.premier_league.data_ingestion_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.example.pl_core_data.entity")          // Path to shared Entities
@EnableJpaRepositories("com.example.pl_core_data.repository")
@ComponentScan(basePackages = {
		"com.premier_league.data_ingestion_service",
		"com.example.pl_core_data"
})
public class DataIngestionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DataIngestionServiceApplication.class, args);
	}

	// Add this method here
	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
}
