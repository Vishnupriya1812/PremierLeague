package com.premier_league.data_ingestion_service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
//import com.premier_league.data_ingestion_service.entity.Match;
//import com.premier_league.data_ingestion_service.entity.Season;
//import com.premier_league.data_ingestion_service.entity.Team;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

@Slf4j
public class testMatches {

    @Test
    public void listAllSeasons() {
        ObjectMapper objectMapper = new ObjectMapper();
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"));
            Page page = context.newPage();
            String seasonsUrl = "https://api.sofascore.com/api/v1/unique-tournament/17/seasons";
            Response response = page.navigate(seasonsUrl);
            if (response != null && response.status() == 200) {
                JsonNode root = objectMapper.readTree(response.text());
                JsonNode seasonsNode = root.get("seasons");
                if (seasonsNode.isArray()) {
                    System.out.println("-------------------------------------------");
                    System.out.printf("%-10s | %-10s%n", "SEASON ID", "YEAR");
                    System.out.println("-------------------------------------------");
                    for (JsonNode node : seasonsNode) {
                        int id = node.get("id").asInt();
                        String year = node.get("year").asText();
                        System.out.printf("%-10d | %-10s%n", id, year);
                    }
                    System.out.println("-------------------------------------------");
                }
            } else {
                log.error("Failed to fetch seasons. Status: {}", response != null ? response.status() : "No Response");
            }
            browser.close();
        } catch (Exception e) {
            log.error("Season listing failed: ", e);
        }
    }

    @Test
    public void ingestAllRoundsForSeason() {
        int targetSeasonId = 61627; // 24/25 Season
        ObjectMapper objectMapper = new ObjectMapper();
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"));
            Page page = context.newPage();

            // 1. Discover all rounds for this season
            String roundsUrl = String.format("https://api.sofascore.com/api/v1/unique-tournament/17/season/%d/rounds", targetSeasonId);
            Response roundsResp = page.navigate(roundsUrl);
            JsonNode roundsRoot = objectMapper.readTree(roundsResp.text());
            JsonNode roundsArray = roundsRoot.get("rounds");

            log.info(">>> Found {} rounds for season {}", roundsArray.size(), targetSeasonId);

            for (JsonNode roundObj : roundsArray) {
                int roundNumber = roundObj.get("round").asInt();
                log.info(">>> PROCESSING ROUND: {}", roundNumber);

                // 2. Fetch matches for this specific round
                String eventsUrl = String.format("https://api.sofascore.com/api/v1/unique-tournament/17/season/%d/events/round/%d", targetSeasonId, roundNumber);
                Response eventsResp = page.navigate(eventsUrl);
                JsonNode events = objectMapper.readTree(eventsResp.text()).get("events");

                for (JsonNode event : events) {
                    int matchId = event.get("id").asInt();

                    // 3. Skip if already in DB (Safety check)
//                    if (matchRepository.existsById(matchId)) continue;

                    // 4. Fetch Deep Details for Referee, Venue, and Period Scores
                    String detailUrl = "https://api.sofascore.com/api/v1/event/" + matchId;
                    Response detailResp = page.navigate(detailUrl);
                    JsonNode detail = objectMapper.readTree(detailResp.text()).get("event");
                    System.out.println("MATCH DETAILS : "+detail.toPrettyString());
                    // 5. Process and Save
//                    saveMatchWithDetails(event, detail, roundNumber);

                    // Small random delay between deep detail calls (500ms - 1s)
                    Thread.sleep((long) (Math.random() * 500) + 500);
                    break;
                }

                // Slightly longer pause between rounds to look human
                Thread.sleep(2000);
                break;
            }

            browser.close();
        } catch (Exception e) {
            log.error("Batch ingestion failed: ", e);
        }
    }
}
