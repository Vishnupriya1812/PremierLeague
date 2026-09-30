package com.premierleague.live_match_tracker;

import com.example.pl_core_data.entity.*;
import com.example.pl_core_data.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j

public class SofaScoreScraperTest {
    //    14025135
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String BASE_API = "https://api.sofascore.com/api/v1";
    private final TeamRepository teamRepository;
    private final SeasonRepository seasonRepository;

    public SofaScoreScraperTest(TeamRepository teamRepository, SeasonRepository seasonRepository) {
        this.teamRepository = teamRepository;
        this.seasonRepository = seasonRepository;
    }

    @Test
    public void ingestAllMatches() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36")
                    .setExtraHTTPHeaders(Map.of(
                            "Sec-CH-UA", "\"Not(A:Brand\";v=\"99\", \"Google Chrome\";v=\"144\", \"Chromium\";v=\"144\"",
                            "Sec-CH-UA-Mobile", "?0",
                            "Sec-CH-UA-Platform", "\"Windows\"",
                            "Sec-Fetch-Dest", "document",
                            "Sec-Fetch-Mode", "navigate",
                            "Sec-Fetch-Site", "none",
                            "Sec-Fetch-User", "?1",
                            "Upgrade-Insecure-Requests", "1"
                    ))
                    .setViewportSize(1920, 1080)
                    .setScreenSize(1920, 1080)
                    .setLocale("en-US")
                    .setTimezoneId("Asia/Kolkata")
                    .setExtraHTTPHeaders(Map.of(
                            "Accept", "application/json, text/plain, */*",
                            "Accept-Language", "en-US,en;q=0.9",
                            "Referer", "https://www.sofascore.com/",
                            "Sec-Fetch-Dest", "empty",
                            "Sec-Fetch-Mode", "cors",
                            "Sec-Fetch-Site", "same-origin"
                    ))
                    .setJavaScriptEnabled(true)
            );
            Page page = context.newPage();
            processSingleMatch(page,12436985);
            browser.close();
        } catch (Exception e) {
            log.error("Ingestion failed: ", e);
        }


    }

    private void processSingleMatch(Page page, int matchId) throws Exception {
        System.out.println("HERE");
        String matchURL = "https://api.sofascore.com/api/v1/event/14566935/";
        JsonNode matchData = fetchJson(page,matchURL);
        System.out.println("SEASON : "+matchData.path("event").path("season").toPrettyString());
        MatchLineup matchLineup = new MatchLineup();
        Match match = new Match();
        match.setId(matchId);
        match.setAttendance(matchData.path("event").path("venue").path("capacity").asInt());
        match.setAwayScore(matchData.path("event").path("awayScore").path("current").asInt());
        match.setHomeScore(matchData.path("event").path("homeScore").path("current").asInt());
        match.setHtAwayScore(matchData.path("event").path("ht_away_score").path("period1").asInt());
        match.setHtHomeScore(matchData.path("event").path("ht_home_score").path("period1").asInt());
        match.setKickoffTime(matchData.path("event").path("startTimestamp").asLong());
        match.setReferee(matchData.path("event").path("referee").path("name").asText());
        match.setRound(matchData.path("event").path("roundInfo").path("round").asInt());
        match.setStatus(matchData.path("event").path("status").path("type").asText());
        match.setVenue(matchData.path("event").path("venue").path("city").asText() + ", "+
                       matchData.path("event").path("venue").path("stadium").path("name").asText());
        match.setHomeTeam(teamRepository.findByNameIgnoreCase(matchData.path("event").path("homeTeam").path("name").asText()).get());
        match.setAwayTeam(teamRepository.findByNameIgnoreCase(matchData.path("event").path("awayTeam").path("name").asText()).get());
        match.setSeason(seasonRepository.findByYear(matchData.path("event").path("season").path("year").asText()).get());
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(match));
    }

    private JsonNode fetchJson(Page page, String url) throws Exception {
        Response response = page.navigate(url);
        return (response != null && response.status() == 200) ? mapper.readTree(response.text()) : mapper.createObjectNode();
    }

}
