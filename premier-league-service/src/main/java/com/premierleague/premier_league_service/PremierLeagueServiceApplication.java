package com.premierleague.premier_league_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.example.pl_core_data.entity")
@EnableJpaRepositories(basePackages = "com.example.pl_core_data.repository") // <--- ADD THIS BACK
@ComponentScan(basePackages = {
		"com.premierleague.premier_league_service", // Scans your Impl, Kafka, Services
		"com.example.pl_core_data"                   // Scans Core Data
})
public class PremierLeagueServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PremierLeagueServiceApplication.class, args);
	}

}
