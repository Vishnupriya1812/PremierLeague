package com.premierleague.live_match_tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.example.pl_core_data.entity")
@EnableJpaRepositories(basePackages = "com.example.pl_core_data.repository") // <--- ADD THIS BACK
@ComponentScan(basePackages = {// Scans your Impl, Kafka, Services
		"com.example.pl_core_data",
		"com.premierleague.live_match_tracker"
})
public class LiveMatchTrackerApplication {
    public static void main(String[] args) {
		SpringApplication.run(LiveMatchTrackerApplication.class, args);
	}

}
