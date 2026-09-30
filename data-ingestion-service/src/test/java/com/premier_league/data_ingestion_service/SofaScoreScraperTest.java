package com.premier_league.data_ingestion_service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;

@Slf4j
public class SofaScoreScraperTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private static final String BASE_API = "https://api.sofascore.com/api/v1";


    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final int UCL_TOURNAMENT_ID = 7;

    @Test
    public void printRound7MatchData() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            );
            Page page = context.newPage();

            // 1. Get Season ID for 25/26
            String seasonsUrl = "https://api.sofascore.com/api/v1/unique-tournament/" + UCL_TOURNAMENT_ID + "/seasons";
            Response seasonsResp = page.navigate(seasonsUrl);
            JsonNode seasonsRoot = objectMapper.readTree(seasonsResp.text());

            // Extract the most recent Season ID (25/26)
            int seasonId = seasonsRoot.get("seasons").get(0).get("id").asInt();
            System.out.println("-------------------------------------------");
            System.out.println("✅ TARGET SEASON ID (25/26): " + seasonId);
            System.out.println("-------------------------------------------");

            // 2. Fetch Round 7 Events
            int round = 7;
            String eventsUrl = String.format("https://api.sofascore.com/api/v1/unique-tournament/%d/season/%d/events/round/%d",
                    UCL_TOURNAMENT_ID, seasonId, round);

            Response eventsResponse = page.navigate(eventsUrl);

            if (eventsResponse.status() == 200) {
                JsonNode eventsNode = objectMapper.readTree(eventsResponse.text()).get("events");

                System.out.printf("%-10s | %-25s | %-25s%n", "MATCH ID", "HOME TEAM", "AWAY TEAM");
                System.out.println("----------------------------------------------------------------------");

                for (JsonNode event : eventsNode) {
                    int matchId = event.get("id").asInt();
                    String home = event.get("homeTeam").get("name").asText();
                    String away = event.get("awayTeam").get("name").asText();

                    // This prints exactly what you need for your Kafka Producer
                    System.out.printf("%-10d | %-25s | %-25s%n", matchId, home, away);
                }
            } else {
                System.out.println("Failed to fetch events. Status code: {}"+eventsResponse.status());
            }

            browser.close();
        } catch (Exception e) {

            System.out.println("Discovery failed: ");
        }
    }


    void testFullDataPipeline() {
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
            Page page = context.newPage();System.out.println(">>> Initializing Session...");
            processSeasonRounds(page,1222);
            browser.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processSeasonRounds(Page page, int seasonId) throws Exception {
        for (int r = 1; r <= 38; r++) {
            System.out.println("\n--- ROUND " + r + " ---");
            String url = String.format("%s/unique-tournament/17/season/%d/events/round/%d", BASE_API, seasonId, r);
            JsonNode events = fetchJson(page, url).path("events");
            if (events.isMissingNode() || events.size() == 0) continue;
            for (JsonNode event : events) {
                fetchDetailedMatchData(page, event.path("id").asInt(),
                        event.at("/homeTeam/name").asText(),
                        event.at("/awayTeam/name").asText());
                break; // Testing: one match per round
            }
            break; // Testing: one round only
        }
    }

    private void fetchDetailedMatchData(Page page, int matchId, String home, String away) throws Exception {
        String eventUrl = BASE_API + "/event/" + matchId;
        JsonNode eventRoot = fetchJson(page, eventUrl).path("event");

        // 1. METADATA
        printMatchHeader(eventRoot, home, away);

        // 2. LINEUPS & PLAYER OF THE MATCH
        JsonNode lineupData = fetchJson(page, eventUrl + "/lineups");
        processTeamLineup(lineupData.at("/home/players"), home);
        processTeamLineup(lineupData.at("/away/players"), away);

        // 3. STATISTICS (Full Match)
        JsonNode statsArray = fetchJson(page, eventUrl + "/statistics").path("statistics").get(0);
//        System.out.println(statsArray.toPrettyString());
//        return;
        printFullStats(statsArray);

        // 4. TIMELINE
        JsonNode incidents = fetchJson(page, eventUrl + "/incidents").path("incidents");
        printTimeline(incidents, home, away);
    }


    private void findAndPrintDetailedStats(JsonNode lineupData, String targetName) {
        for (String side : Arrays.asList("home", "away")) {
            for (JsonNode p : lineupData.at("/" + side + "/players")) {
                if (p.at("/player/name").asText().contains(targetName)) {
                    JsonNode s = p.path("statistics");
                    System.out.printf("   Rating: %.1f | Touches: %s | Acc. Passes: %s/%s | Key Passes: %s%n",
                            s.path("rating").asDouble(), s.path("touches").asText(),
                            s.path("accuratePass").asText(), s.path("totalPass").asText(),
                            s.path("keyPass").asText("0"));
                    return;
                }
            }
        }
    }

    private void printMatchHeader(JsonNode event, String home, String away) {
        String matchDate = Instant.ofEpochSecond(event.path("startTimestamp").asLong())
                .atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        System.out.println("\n" + "=".repeat(60));
        System.out.printf("MATCH: %s %d - %d %s%n", home, event.at("/homeScore/display").asInt(), event.at("/awayScore/display").asInt(), away);
        System.out.printf("VENUE: %s | REF: %s | ATT: %d | DATE: %s%n",
                event.at("/venue/name").asText("N/A"), event.at("/referee/name").asText("N/A"), event.path("attendance").asInt(0), matchDate);
        System.out.println("=".repeat(60));
    }

    private void processTeamLineup(JsonNode players, String teamName) {
        System.out.println("\n[" + teamName + " Lineup]");
        for (JsonNode p : players) {
//            System.out.println(p.toPrettyString());
            System.out.printf("  %-25s | Rating: %.1f %s%n",
                    p.at("/player/name").asText(), p.at("/statistics/rating").asDouble(0.0),
                    p.path("substitute").asBoolean() ? "[Sub]" : "");
        }
    }

    private void printFullStats(JsonNode statsArray) {
        if (!statsArray.isArray()) return;
        for (JsonNode period : statsArray) {
            if ("ALL".equals(period.path("period").asText())) {
                System.out.println("\n--- FULL MATCH STATISTICS ---");
                for (JsonNode group : period.path("groups")) {
                    for (JsonNode item : group.path("statisticsItems")) {
                        System.out.printf("  %-22s: %s - %s%n", item.path("name").asText(), item.path("home").asText(), item.path("away").asText());
                    }
                }
            }
        }
    }

    private void printTimeline(JsonNode incidents, String home, String away) {
        System.out.println("\n--- MATCH TIMELINE ---");
        System.out.println(incidents.toPrettyString());
        for (JsonNode inc : incidents) {
//            String team = inc.path("isHome").asBoolean() ? home : away;
//            String type = inc.path("incidentType").asText();
//            int time = inc.path("time").asInt();
//
//            if ("goal".equals(type)) {
//                System.out.printf("[%d'] GOAL (%s): %s (%d-%d)%n", time, team, inc.at("/player/name").asText(), inc.path("homeScore").asInt(), inc.path("awayScore").asInt());
//            } else if ("substitution".equals(type)) {
//                System.out.printf("[%d'] SUB (%s): %s IN / %s OUT%n", time, team, inc.at("/playerIn/name").asText(), inc.at("/playerOut/name").asText());
//            } else if ("card".equals(type)) {
//                System.out.printf("[%d'] %s CARD (%s): %s%n", time, inc.path("incidentClass").asText().toUpperCase(), team, inc.at("/player/name").asText());
//            }
        }
    }

    private JsonNode fetchJson(Page page, String url) throws Exception {
        Response response = page.navigate(url);
        return (response != null && response.status() == 200) ? mapper.readTree(response.text()) : mapper.createObjectNode();
    }
}