package com.premier_league.data_ingestion_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.example.pl_core_data.entity.*;
import com.example.pl_core_data.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MatchDiscoveryService {

    private final SeasonRepository seasonRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository; // You'll need this to link teams
    private final ObjectMapper objectMapper;

    public MatchDiscoveryService(SeasonRepository seasonRepository, MatchRepository matchRepository,
                                 TeamRepository teamRepository, ObjectMapper objectMapper) {
        this.seasonRepository = seasonRepository;
        this.matchRepository = matchRepository;
        this.teamRepository = teamRepository;
        this.objectMapper = objectMapper;
    }

    public void ingestAllMatches() {
        List<String> historicalYears = Arrays.asList(
//                "25/26"
//                "24/25"
//                "23/24"
//                "22/23"
                "21/22"
//              , "20/21", "19/20", "18/19", "17/18", "16/17", "15/16",
//                "14/15", "13/14", "12/13", "11/12", "10/11", "09/10", "08/09", "07/08", "06/07", "05/06",
//                "04/05", "03/04", "02/03", "01/02"
        );

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    // 1. Set a modern User Agent
                    .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")

                    // 2. Set realistic screen dimensions
                    .setViewportSize(1920, 1080)
                    .setScreenSize(1920, 1080)

                    // 3. Match the language to your IP location
                    .setLocale("en-US")
                    .setTimezoneId("Asia/Kolkata") // Adjust to your actual timezone

                    // 4. Critical: Set extra HTTP headers
                    .setExtraHTTPHeaders(Map.of(
                            "Accept", "application/json, text/plain, */*",
                            "Accept-Language", "en-US,en;q=0.9",
                            "Referer", "https://www.sofascore.com/",
                            "Sec-Fetch-Dest", "empty",
                            "Sec-Fetch-Mode", "cors",
                            "Sec-Fetch-Site", "same-origin"
                    ))

                    // 5. Enable JavaScript (Cloudflare requires it)
                    .setJavaScriptEnabled(true)
            );
            Page page = context.newPage();

            List<Season> targetSeasons = seasonRepository.findByYearIn(historicalYears);

            for (Season season : targetSeasons) {
                String roundsUrl = "https://api.sofascore.com/api/v1/unique-tournament/17/season/" + season.getId() + "/rounds";
                Response roundsResponse = page.navigate(roundsUrl);
                JsonNode roundsRoot = objectMapper.readTree(roundsResponse.text());
                JsonNode roundsNode = roundsRoot.get("rounds");
                for (JsonNode roundObj : roundsNode) {
                    int r = roundObj.get("round").asInt();
                    String eventsUrl = String.format("https://api.sofascore.com/api/v1/unique-tournament/17/season/%d/events/round/%d", season.getId(), r);
                    Response eventsResponse = page.navigate(eventsUrl);
                    if (eventsResponse.status() == 200) {
                        processEvents(objectMapper.readTree(eventsResponse.text()).get("events"), season,r,page);
                        long delay = (long) (Math.random() * 1000) + 500;
                        Thread.sleep(delay);
                    }
                }
                Thread.sleep(10000);
            }
            browser.close();
        } catch (Exception e) {
            log.error("Ingestion failed: ", e);
        }
    }
    private void processEvents(JsonNode eventsNode, Season season, int round, Page page) {
        for (JsonNode event : eventsNode) {
            int matchId = event.get("id").asInt();
            if (matchRepository.existsById(matchId)) continue;
            String detailUrl = "https://api.sofascore.com/api/v1/event/" + matchId;
            Response detailResp = page.navigate(detailUrl);
            JsonNode detail = null;
            try {
                detail = objectMapper.readTree(detailResp.text()).get("event");
            } catch (Exception e) {
                log.error("Failed to parse details for match {}", matchId);
                continue;
            }
            Team homeTeam = findOrCreateTeam(event.get("homeTeam"));
            Team awayTeam = findOrCreateTeam(event.get("awayTeam"));
            Match match = new Match();
            match.setId(matchId);
            match.setSeason(season);
            match.setRound(round);
            match.setHomeTeam(homeTeam);
            match.setAwayTeam(awayTeam);
            JsonNode hScore = detail.path("homeScore");
            JsonNode aScore = detail.path("awayScore");
            match.setHomeScore(hScore.path("current").asInt(0));
            match.setAwayScore(aScore.path("current").asInt(0));
            match.setHtHomeScore(hScore.path("period1").asInt(0));
            match.setHtAwayScore(aScore.path("period1").asInt(0));
            match.setAttendance(detail.path("attendance").asInt(0));
            String stadium = detail.path("venue").path("name").asText(null);
            String city = detail.path("venue").path("city").path("name").asText(null);
            if (stadium != null && city != null) {
                match.setVenue(stadium + ", " + city);
            } else {
                match.setVenue(stadium);
            }
            if (detail.has("referee")) {
                match.setReferee(detail.path("referee").path("name").asText());
            }
            // Directly set the long value from the JsonNode
            match.setKickoffTime(detail.get("startTimestamp").asLong());
            match.setStatus(detail.path("status").path("type").asText("unknown"));
            matchRepository.save(match);
            log.info("Saved Deep Data for Match: {} - {} vs {}", matchId, homeTeam.getName(), awayTeam.getName());
            try {
                Thread.sleep(500);
            }
            catch (InterruptedException ignored)
            {
                log.error("Interrupted Exception");
            }
        }
    }
    private Team findOrCreateTeam(JsonNode teamNode) {
        int teamId = teamNode.get("id").asInt();
        return teamRepository.findById(teamId).orElseGet(() -> {
            Team newTeam = new Team();
            newTeam.setId(teamId);
            newTeam.setName(teamNode.get("name").asText());
            newTeam.setShortName(teamNode.has("shortName") ? teamNode.get("shortName").asText() : null);
            String stadiumName = teamNode.path("venue").path("name").asText(null);
            newTeam.setStadiumName(stadiumName);
            log.info("Discovered New Team: {} (Stadium: {})", newTeam.getName(), stadiumName);
            return teamRepository.save(newTeam);
        });
    }
}
