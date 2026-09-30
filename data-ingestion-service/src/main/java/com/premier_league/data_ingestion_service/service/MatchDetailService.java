package com.premier_league.data_ingestion_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.example.pl_core_data.entity.*;
import com.example.pl_core_data.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class MatchDetailService {

    private final SeasonRepository seasonRepository;
    private final MatchLineupRepository matchLineupRepository;
    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final PlayerMatchStatisticsRepository playerMatchStatisticsRepository;
    private final TeamMatchStatisticsRepository teamMatchStatisticsRepository;
    private final MatchIncidentRepository matchIncidentRepository;
    private final LeagueStandingsService leagueStandingsService;
    private final ObjectMapper objectMapper;

    private static final String BASE_API = "https://api.sofascore.com/api/v1";

    public MatchDetailService(SeasonRepository seasonRepository, MatchLineupRepository matchLineupRepository,
                              PlayerRepository playerRepository, MatchRepository matchRepository,
                              TeamRepository teamRepository, PlayerMatchStatisticsRepository playerMatchStatisticsRepository,
                              TeamMatchStatisticsRepository teamMatchStatisticsRepository,
                              MatchIncidentRepository matchIncidentRepository, ObjectMapper objectMapper,
                              LeagueStandingsService leagueStandingsService) {
        this.seasonRepository = seasonRepository;
        this.matchLineupRepository = matchLineupRepository;
        this.playerRepository = playerRepository;
        this.matchRepository = matchRepository;
        this.teamRepository = teamRepository;
        this.playerMatchStatisticsRepository = playerMatchStatisticsRepository;
        this.teamMatchStatisticsRepository = teamMatchStatisticsRepository;
        this.matchIncidentRepository = matchIncidentRepository;
        this.objectMapper = objectMapper;
        this.leagueStandingsService = leagueStandingsService;
    }

    public void ingestAllMatches() {
        List<String> historicalYears = List.of("21/22");

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

            List<Season> targetSeasons = seasonRepository.findByYearIn(historicalYears);
            for (Season season : targetSeasons) {
                processSeasonRounds(page, season);
            }
            browser.close();
        } catch (Exception e) {
            log.error("Ingestion failed: ", e);
        }
    }

    private void processSeasonRounds(Page page, Season season) throws Exception {
        String roundsUrl = String.format("%s/unique-tournament/17/season/%d/rounds", BASE_API, season.getId());
        JsonNode roundsRoot = fetchJson(page, roundsUrl);
        JsonNode roundsNode = roundsRoot.path("rounds");
        for (JsonNode roundObj : roundsNode) {
            int round = roundObj.get("round").asInt();
            log.info("--- Processing Season {} | Round {} ---", season.getYear(), round);
            String eventsUrl = String.format("%s/unique-tournament/17/season/%d/events/round/%d", BASE_API, season.getId(), round);
            JsonNode events = fetchJson(page, eventsUrl).path("events");
            if (events.isMissingNode() || events.isEmpty()) continue;
            for (JsonNode event : events) {
                int matchId = event.path("id").asInt();
                processSingleMatch(page, matchId, event, round);
                Thread.sleep(ThreadLocalRandom.current().nextInt(2000, 3000));
            }
            Thread.sleep(ThreadLocalRandom.current().nextInt(10000, 15000));
        }
    }

    private void processSingleMatch(Page page, int matchId, JsonNode event, int round) {
        String statusType = event.path("status").path("type").asText();
        log.info("Processing Match: {} vs {} | Status: {}",
                event.at("/homeTeam/name").asText(),
                event.at("/awayTeam/name").asText(),
                statusType);
        if (!"finished".equalsIgnoreCase(statusType)) {
            log.info("Skipping match ID {}: Match is {}", matchId, statusType);
            return;
        }
        try {
            fetchDetailedMatchData(page, matchId, event.path("homeTeam"), event.path("awayTeam"));
            String statsUrl = String.format("%s/event/%d/statistics", BASE_API, matchId);
            JsonNode statsResponse = fetchJson(page, statsUrl);
            if (statsResponse.has("statistics")) {
                fetchMatchStatistics(statsResponse.path("statistics").get(0).path("groups"), matchId, event);
            }
            String incidentsUrl = String.format("%s/event/%d/incidents", BASE_API, matchId);
            fetchMatchIncidents(fetchJson(page, incidentsUrl).path("incidents"), matchId, event.path("homeTeam"), event.path("awayTeam"));
            Match match = matchRepository.findById(matchId)
                    .orElseThrow(() -> new RuntimeException("Match not found: " + matchId));
            leagueStandingsService.updateStandings(match, match.getHomeTeam(), match.getHomeScore(), match.getAwayScore(), round);
            leagueStandingsService.updateStandings(match, match.getAwayTeam(), match.getAwayScore(), match.getHomeScore(), round);

        } catch (Exception e) {
            log.error("Failed to process match {}: {}", matchId, e.getMessage());
        }
    }
    @Transactional
    protected void fetchDetailedMatchData(Page page, int matchId, JsonNode homeTeam, JsonNode awayTeam) throws Exception {
        String lineupUrl = String.format("%s/event/%d/lineups", BASE_API, matchId);
        JsonNode lineupData = fetchJson(page, lineupUrl);
        processTeamLineup(lineupData.at("/home/players"), homeTeam, matchId);
        processTeamLineup(lineupData.at("/away/players"), awayTeam, matchId);
    }

    private void processTeamLineup(JsonNode players, JsonNode teamNode, int matchId) {
        Team team = teamRepository.getReferenceById(teamNode.path("id").asInt());
        Match match = matchRepository.getReferenceById(matchId);
        for (JsonNode p : players) {
            JsonNode playerData = p.path("player");
            Player player = findOrCreatePlayer(playerData);
            if (player == null) continue;
            if (!matchLineupRepository.existsByMatchIdAndPlayerId(matchId, player.getId())) {
                MatchLineup lineup = new MatchLineup();
                lineup.setMatch(match);
                lineup.setPlayer(player);
                lineup.setTeam(team);
                lineup.setIsStarting(!p.path("substitute").asBoolean());
                lineup.setPosition(p.path("position").asText("N/A"));
                lineup.setShirtNumber(p.path("jerseyNumber").asInt(0));
                lineup.setRating(p.path("statistics").path("rating").asDouble(0.0));
                lineup.setCaptain(p.path("captain").asBoolean(false));
                matchLineupRepository.save(lineup);
            }
            if (!playerMatchStatisticsRepository.existsByMatchIdAndPlayerId(matchId, player.getId())) {
                PlayerMatchStatistics stats = new PlayerMatchStatistics();
                stats.setMatch(match);
                stats.setPlayer(player);
                JsonNode s = p.path("statistics");
                stats.setMinutesPlayed(s.path("minutesPlayed").asInt(0));
                stats.setAssists(s.path("goalAssist").asInt(0));
                stats.setPasses(s.path("totalPass").asInt(0));
                stats.setTackles(s.path("totalTackle").asInt(0));
                stats.setShots(s.path("totalShots").asInt(0));
                stats.setSaves(s.path("saves").asInt(0));
                stats.setTouches(s.path("touches").asInt(0));
                stats.setGoalsPrevented(s.path("goalsPrevented").asInt(0));
                stats.setGoals(0);
                stats.setYellowCards(0);
                stats.setRedCards(0);
                playerMatchStatisticsRepository.save(stats);
            }
        }
    }
    private Player findOrCreatePlayer(JsonNode playerNode) {
        int playerId = playerNode.path("id").asInt(0);
        if (playerId == 0) return null;
        return playerRepository.findById(playerId).orElseGet(() -> {
            Player newPlayer = new Player();
            newPlayer.setId(playerId);
            newPlayer.setName(playerNode.path("name").asText("Unknown"));
            newPlayer.setPosition(playerNode.path("position").asText("N/A"));
            newPlayer.setHeight(playerNode.path("height").asInt(0));
            newPlayer.setSlug(playerNode.path("slug").asText("unknown"));
            newPlayer.setCountry(playerNode.path("country").path("name").asText("N/A"));
            String marketVal = playerNode.path("proposedMarketValueRaw").path("value").asText("");
            String currency = playerNode.path("marketValueCurrency").asText("");
            newPlayer.setMarketValue(marketVal + " " + currency);
            return playerRepository.save(newPlayer);
        });
    }

    private void fetchMatchIncidents(JsonNode incidents, int matchId, JsonNode homeTeam, JsonNode awayTeam) {
        Team home = teamRepository.getReferenceById(homeTeam.path("id").asInt());
        Team away = teamRepository.getReferenceById(awayTeam.path("id").asInt());
        for (JsonNode incident : incidents) {
            String type = incident.path("incidentType").asText();
            if (!List.of("goal", "substitution", "card").contains(type)) continue;
            if (incident.path("isHome").isMissingNode()) continue;
            MatchIncident mi = new MatchIncident();
            mi.setMatch(matchRepository.getReferenceById(matchId));
            mi.setTeam(incident.path("isHome").asBoolean() ? home : away);
            mi.setType(type);
            mi.setMinute(incident.path("time").asInt());
            try {
                mapIncidentSpecifics(mi, incident, type);
                if (mi.getPlayer() != null && !matchIncidentRepository.existsByMatchIdAndPlayerIdAndTypeAndMinute(
                        matchId, mi.getPlayer().getId(), type, mi.getMinute())) {
                    matchIncidentRepository.save(mi);
                    updatePlayerMatchStats(matchId, mi);
                }
            } catch (Exception e) {
                log.error("Failed to process incident: {} at minute {}", type, mi.getMinute());
            }
        }
    }

    private void mapIncidentSpecifics(MatchIncident mi, JsonNode node, String type) {
        switch (type) {
            case "goal" -> {
                int scorerId = node.path("player").path("id").asInt(0);
                if (scorerId != 0) mi.setPlayer(playerRepository.getReferenceById(scorerId));
            }
            case "substitution" -> {
                int inId = node.path("playerIn").path("id").asInt(0);
                int outId = node.path("playerOut").path("id").asInt(0);
                if (inId != 0) mi.setPlayer(playerRepository.getReferenceById(inId));
                if (outId != 0) mi.setRelatedPlayer(playerRepository.getReferenceById(outId));
            }
            case "card" -> {
                int cardedId = node.path("player").path("id").asInt(0);
                if (cardedId != 0) mi.setPlayer(playerRepository.getReferenceById(cardedId));
                mi.setCardType(node.path("incidentClass").asText());
            }
        }
    }

    private void updatePlayerMatchStats(int matchId, MatchIncident incident) {
        if (incident.getPlayer() == null) return;
        playerMatchStatisticsRepository.findByMatchIdAndPlayerId(matchId, incident.getPlayer().getId())
                .ifPresent(stats -> {
                    if ("goal".equalsIgnoreCase(incident.getType())) {
                        stats.setGoals(stats.getGoals() + 1);
                    } else if ("card".equalsIgnoreCase(incident.getType())) {
                        if ("yellow".equalsIgnoreCase(incident.getCardType())) stats.setYellowCards(stats.getYellowCards() + 1);
                        else if ("red".equalsIgnoreCase(incident.getCardType())) stats.setRedCards(stats.getRedCards() + 1);
                    }
                    playerMatchStatisticsRepository.save(stats);
                });
    }

    private void fetchMatchStatistics(JsonNode statistics, int matchId, JsonNode matchEvent) {
        int homeTeamId = matchEvent.at("/homeTeam/id").asInt();
        int awayTeamId = matchEvent.at("/awayTeam/id").asInt();
        String period = "ALL";
        TeamMatchStatistics homeStats = teamMatchStatisticsRepository
                .findByMatchIdAndTeamIdAndPeriod(matchId, homeTeamId, period)
                .orElseGet(() -> createBaseTeamStats(matchId, homeTeamId, period));
        TeamMatchStatistics awayStats = teamMatchStatisticsRepository
                .findByMatchIdAndTeamIdAndPeriod(matchId, awayTeamId, period)
                .orElseGet(() -> createBaseTeamStats(matchId, awayTeamId, period));
        Map<String, Map<String, JsonNode>> statsMap = new HashMap<>();
        for (JsonNode group : statistics) {
            for (JsonNode item : group.path("statisticsItems")) {
                statsMap.put(item.path("key").asText(), Map.of("h", item.path("homeValue"), "a", item.path("awayValue")));
            }
        }
        populateTeamStats(homeStats, statsMap, "h");
        populateTeamStats(awayStats, statsMap, "a");
        teamMatchStatisticsRepository.save(homeStats);
        teamMatchStatisticsRepository.save(awayStats);
    }

    private TeamMatchStatistics createBaseTeamStats(int matchId, int teamId, String period) {
        TeamMatchStatistics ts = new TeamMatchStatistics();
        ts.setMatch(matchRepository.getReferenceById(matchId));
        ts.setTeam(teamRepository.getReferenceById(teamId));
        ts.setPeriod(period);
        return ts;
    }

    private void populateTeamStats(TeamMatchStatistics stats, Map<String, Map<String, JsonNode>> map, String side) {
        stats.setBallPossession(getVal(map, "ballPossession", side).asInt());
        stats.setExpectedGoals(getVal(map, "expectedGoals", side).asDouble());
        stats.setBigChanceCreated(getVal(map, "bigChanceCreated", side).asInt());
        stats.setCornerKicks(getVal(map, "cornerKicks", side).asInt());
        stats.setFouls(getVal(map, "fouls", side).asInt());
        stats.setPasses(getVal(map, "passes", side).asInt());
        stats.setYellowCards(getVal(map, "yellowCards", side).asInt());
        stats.setShotsOnGoal(getVal(map, "shotsOnGoal", side).asInt());
        stats.setShotsOffGoal(getVal(map, "shotsOffGoal", side).asInt());
        stats.setBigChanceMissed(getVal(map, "bigChanceMissed", side).asInt());
        stats.setOffsides(getVal(map, "offsides", side).asInt());
    }

    private JsonNode getVal(Map<String, Map<String, JsonNode>> map, String key, String side) {
        if (map.containsKey(key) && map.get(key).containsKey(side)) return map.get(key).get(side);
        return com.fasterxml.jackson.databind.node.MissingNode.getInstance();
    }

    private JsonNode fetchJson(Page page, String url) throws JsonProcessingException {
        Response response = page.navigate(url);
        if (response == null) return objectMapper.createObjectNode();
        int status = response.status();
        if (status == 403) {
            log.error("ACCESS FORBIDDEN (403) at URL: {}. Anti-bot triggered.", url);
            return objectMapper.createObjectNode();
        }
        if (status != 200) {
            log.warn("Non-200 response ({}) for URL: {}", status, url);
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(response.text());
    }
}