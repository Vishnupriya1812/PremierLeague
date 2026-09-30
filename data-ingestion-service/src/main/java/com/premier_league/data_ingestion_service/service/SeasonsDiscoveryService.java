package com.premier_league.data_ingestion_service.service;

import com.microsoft.playwright.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.example.pl_core_data.entity.*;
import com.example.pl_core_data.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class SeasonsDiscoveryService {

    private final SeasonRepository seasonRepository;
    private final CompetitionRepository competitionRepository;
    private final ObjectMapper objectMapper;

    public SeasonsDiscoveryService(SeasonRepository seasonRepository, CompetitionRepository competitionRepository, ObjectMapper objectMapper) {
        this.seasonRepository = seasonRepository;
        this.competitionRepository = competitionRepository;
        this.objectMapper = objectMapper;
    }

    public void ingestAllSeasons() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            String apiUrl = "https://api.sofascore.com/api/v1/unique-tournament/17/seasons";
            Response response = page.navigate(apiUrl);
            if (response != null && response.status() == 200) {
                String json = response.text();
                JsonNode root = objectMapper.readTree(json);
                JsonNode seasonsNode = root.get("seasons");
                if (seasonsNode != null && seasonsNode.isArray()) {
                    Competition pl = competitionRepository.findById(17)
                            .orElseThrow(() -> new RuntimeException("Competition 17 not found! Verify your DB."));
                    for (JsonNode node : seasonsNode) {
                        int id = node.get("id").asInt();
                        String year = node.get("year").asText();
                        if (!seasonRepository.existsById(id)) {
                                Season season = new Season();
                                season.setId(id);
                                season.setYear(year);
                                season.setCompetition(pl);
                                seasonRepository.save(season);
                            log.info("Saved Season: {} (ID: {})", year, id);
                            }

                    }
                }
            } else {
                log.error("Failed to fetch data. Status: {}", response != null ? response.status() : "null");
            }
            browser.close();
        } catch (Exception e) {
            log.error("Error during season ingestion: {}", e.getMessage());
        }
    }
}