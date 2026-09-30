package com.premierleague.live_match_tracker.service;

import com.example.pl_core_data.entity.*;
import com.example.pl_core_data.model.KafkaMatchEvent;
import com.example.pl_core_data.model.LeagueStandingsUpdation;
import com.example.pl_core_data.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.premierleague.live_match_tracker.kafka.KafkaProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LiveMatchData  {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    private final TeamRepository teamRepository;
    private final SeasonRepository seasonRepository;
    private final KafkaProducer kafkaProducer;
    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;
    private final CompetitionRepository competitionRepository;
    private Playwright playwright;
    private Browser browser;

    public LiveMatchData(TeamRepository teamRepository, SeasonRepository seasonRepository, KafkaProducer kafkaProducer, MatchRepository matchRepository, PlayerRepository playerRepository, MatchLineupRepository matchLineupRepository, CompetitionRepository competitionRepository, MatchIncidentRepository matchIncidentRepository, PlayerMatchStatisticsRepository playerMatchStatisticsRepository, TeamMatchStatisticsRepository teamMatchStatisticsRepository, LeagueStandingsService leagueStandingsService) {
        this.teamRepository = teamRepository;
        this.seasonRepository = seasonRepository;
        this.kafkaProducer = kafkaProducer;
        this.matchRepository = matchRepository;
        this.playerRepository = playerRepository;
        this.competitionRepository = competitionRepository;
    }

    public void initBrowser() {
        if (playwright == null) {
            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            log.info("Playwright Browser Started");
        }
    }

    public void runScrapeCycle(int round) {
        log.info("Starting Scrape Cycle for Match 14025089...");
        try {
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36...")
            );
            Page page = context.newPage();
            List<Integer> matchIds = matchRepository.findMatchIdsByRoundAndSeasonId(round,76986);
            for (int matchId : matchIds) {
                System.out.println(matchId);
            }
            for (int matchId : matchIds) {
                String matchURL = "https://api.sofascore.com/api/v1/event/" + matchId + "/";
                JsonNode matchData = fetchJson(page, matchURL);
                String status = matchData.path("event").path("status").path("type").asText();
                if (status.equals("finished") || status.equals("inprogress")) {
                    processMatch(page, matchId,matchData);
                }
            }
            page.close();
            context.close();
        } catch (Exception e) {
            log.error("Scrape cycle failed: ", e);
        }
    }

    public void shutdownBrowser() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
        playwright = null;
        log.info("Playwright Browser Shutdown");
    }

    private void processMatch(Page page, int matchId,JsonNode matchData) throws Exception {
        String graphURL = "https://api.sofascore.com/api/v1/event/" + matchId + "/graph";
        JsonNode graphData = fetchJson(page, graphURL);

        Thread.sleep(2000); // Sleep for 2 seconds to mimic human behavior
        log.info("Processing Match ID: {}", matchId);
        processMatchDetails(matchData,graphData, matchId);

        String lineupsURL = "https://api.sofascore.com/api/v1/event/" + matchId + "/lineups";
        JsonNode lineupData = fetchJson(page, lineupsURL);

        Thread.sleep(2000);
        log.info("Processing Lineups for Match ID: {}", matchId);
        processLineUp(lineupData,matchId);

        String incidentURL = "https://api.sofascore.com/api/v1/event/" + matchId + "/incidents";
        JsonNode incidentData = fetchJson(page, incidentURL).path("incidents");

        Thread.sleep(2000);
        log.info("Processing Incidents for Match ID: {}", matchId);
        processMatchIncidents(matchData,incidentData, matchId);

        String statisticsURL = "https://api.sofascore.com/api/v1/event/" + matchId + "/statistics";
        JsonNode statisticsData = fetchJson(page, statisticsURL).path("statistics").get(0).path("groups");

        Thread.sleep(2000);
        log.info("Processing Match Statistics for Match ID: {}", matchId);
        processTeamMatchStatistics(matchData,statisticsData,matchId);

        Thread.sleep(2000);
        log.info("Processing League Standings Update for Match ID: {}", matchId);
        processLeagueStandings(matchData,page,matchId);
    }

    private void processMatchDetails(JsonNode matchData,JsonNode graphData, int matchId) throws Exception {
        JsonNode eventNode = matchData.path("event");
        Match match = new Match();
        match.setId(matchId);
        match.setAttendance(eventNode.path("venue").path("capacity").asInt());
        match.setHomeScore(eventNode.path("homeScore").path("current").asInt());
        match.setAwayScore(eventNode.path("awayScore").path("current").asInt());
        match.setHtAwayScore(eventNode.path("homeScore").path("period1").asInt());
        match.setHtHomeScore(eventNode.path("awayScore").path("period1").asInt());
        match.setKickoffTime(eventNode.path("startTimestamp").asLong());
        match.setReferee(eventNode.path("referee").path("name").asText());
        match.setRound(eventNode.path("roundInfo").path("round").asInt());
        match.setStatus(eventNode.path("status").path("type").asText());
        String venueDetails = eventNode.path("venue").path("city").asText() + ", " +
                eventNode.path("venue").path("stadium").path("name").asText();
        match.setVenue(venueDetails);
        String homeTeamName = eventNode.path("homeTeam").path("name").asText();
        String awayTeamName = eventNode.path("awayTeam").path("name").asText();
        String seasonYear = eventNode.path("season").path("year").asText();
        Team homeTeam = teamRepository.findByNameIgnoreCase(homeTeamName)
                .orElseGet(() -> {
                    Team t = new Team();
                    t.setId(eventNode.path("homeTeam").path("id").asInt());
                    t.setName(eventNode.path("homeTeam").path("name").asText());
                    t.setShortName(eventNode.path("homeTeam").path("slug").asText());
                    t.setStadiumName(eventNode.path("homeTeam").path("stadium").path("name").asText());
                    return teamRepository.save(t);
                });
        match.setHomeTeam(homeTeam);
        Team awayTeam = teamRepository.findByNameIgnoreCase(awayTeamName)
                .orElseGet(() -> {
                    Team t = new Team();
                    t.setId(eventNode.path("awayTeam").path("id").asInt());
                    t.setName(eventNode.path("awayTeam").path("name").asText());
                    t.setShortName(eventNode.path("awayTeam").path("slug").asText());
                    t.setStadiumName(eventNode.path("awayTeam").path("stadium").path("name").asText());
                    return teamRepository.save(t);
                });
        match.setAwayTeam(awayTeam);
        Season season = seasonRepository.findByYear(seasonYear)
                .orElseGet(() -> {
                    Season s = new Season();
                    s.setId(eventNode.path("season").path("id").asInt());
                    s.setYear(seasonYear);
                    s.setCompetition(competitionRepository.findById(17).get());
                    return seasonRepository.save(s);
                });
        match.setSeason(season);
        if (!graphData.path("graphPoints").isMissingNode() )
        {
            JsonNode graphPoints = graphData.path("graphPoints");
            if(graphPoints.isArray() && !graphPoints.isEmpty()) {
                JsonNode lastPoint = graphPoints.get(graphPoints.size() - 1);
                JsonNode currentMinute = lastPoint.path("minute");
                match.setMinute(currentMinute.asDouble());
            }
        }
        kafkaProducer.sendEvent(new KafkaMatchEvent("MATCH_DETAILS",matchId,match));
    }

    private void processLineUp(JsonNode lineupData, int matchId) throws Exception {
        if (lineupData.isMissingNode() || !lineupData.has("home")) {
            log.warn("No lineup data for match: {}", matchId);
            return;
        }
        Match match = matchRepository.findById(matchId).get();
        int homeTeamId = lineupData.path("home").path("players").get(0).path("teamId").asInt();
        processTeamPlayers(lineupData.path("home"), matchId, homeTeamId);
        processPlayerMatchStatistics(lineupData.path("home").path("players"),match,matchId);
        int awayTeamId = lineupData.path("away").path("players").get(0).path("teamId").asInt();
        processTeamPlayers(lineupData.path("away"), matchId, awayTeamId);
        processPlayerMatchStatistics(lineupData.path("away").path("players"),match,matchId);
    }

    private void processTeamPlayers(JsonNode teamNode, int matchId, int teamId) throws JsonProcessingException {
        parsePlayerArray(teamNode.path("players"), matchId, teamId, true);
        parsePlayerArray(teamNode.path("substitutes"), matchId, teamId, false);
    }

    private void parsePlayerArray(JsonNode playersNode, int matchId, int teamId, boolean isStarting) throws JsonProcessingException {
        for (JsonNode node : playersNode) {
            JsonNode pNode = node.path("player");
            int playerId = pNode.path("id").asInt();
            if (!playerRepository.existsById(playerId)) {
                Player p = new Player();
                p.setId(playerId);
                p.setName(pNode.path("name").asText());
                p.setSlug(pNode.path("shortName").asText());
                p.setPosition(pNode.path("position").asText());
                playerRepository.save(p); // Basic info, can be updated later
            }
            Double rating = node.path("statistics").path("rating").asDouble(0.0);
            String position = node.path("position").asText(pNode.path("position").asText());
            int shirtNumber = node.path("shirtNumber").asInt(0);
            boolean isCaptain = node.path("captain").asBoolean(false);
            MatchLineup matchLineup = new MatchLineup();
            matchLineup.setPlayer(playerRepository.findById(playerId).get());
            matchLineup.setMatch(matchRepository.findById(matchId).get());
            matchLineup.setCaptain(isCaptain);
            matchLineup.setRating(rating);
            matchLineup.setIsStarting(!node.path("substitute").asBoolean());
            matchLineup.setTeam(teamRepository.findById(teamId).get());
            matchLineup.setShirtNumber(shirtNumber);
            matchLineup.setPosition(position);
            kafkaProducer.sendEvent(new KafkaMatchEvent("MATCH_LINEUP_ENTRY",matchId,matchLineup));
        }
    }

    private void processMatchIncidents(JsonNode matchData,JsonNode incidentData, int matchId) throws Exception {
        int homeTeamId = matchData.path("event").path("homeTeam").path("id").asInt();
        int awayTeamId = matchData.path("event").path("awayTeam").path("id").asInt();
        Team homeTeam = teamRepository.findById(homeTeamId).get();
        Team awayTeam = teamRepository.findById(awayTeamId).get();
        Match match = matchRepository.findById(matchId).get();
        for(JsonNode incident : incidentData)
        {
            MatchIncident matchIncident = new MatchIncident();
            String incidentType = incident.path("incidentType").asText();
            System.out.println("INCIDENT TYPE : "+incidentType);
            switch (incidentType)
            {
                case "period" :
                    matchIncident.setType("Period");
                    matchIncident.setText(incident.path("text").asText());
                    matchIncident.setMatch(match);
                    matchIncident.setMinute(incident.path("time").asInt());
                    matchIncident.setHomeScore(incident.path("homeScore").asInt());
                    matchIncident.setAwayScore(incident.path("awayScore").asInt());
                    break;
                case "injuryTime" :
                    matchIncident.setType("Injury time");
                    matchIncident.setText(incident.path("length").asText());
                    matchIncident.setMatch(match);
                    matchIncident.setMinute(incident.path("time").asInt());
                    break;
                case "varDecision" :
                    matchIncident.setType("VAR Decision");
                    matchIncident.setMatch(match);
                    matchIncident.setMinute(incident.path("time").asInt());
                    matchIncident.setPlayer(playerRepository.findById(incident.path("player").path("id").asInt()).get());
                    matchIncident.setText(incident.path("incidentClass").asText());
                    matchIncident.setTeam(incident.path("isHome").asBoolean()?homeTeam:awayTeam);
                    break;
                case "goal" :
                    matchIncident.setType("Goal !");
                    matchIncident.setMatch(match);
                    matchIncident.setMinute(incident.path("addedTime").asInt()+incident.path("time").asInt());
                    matchIncident.setText(incident.path("incidentClass").asText());
                    matchIncident.setPlayer(playerRepository.findById(incident.path("player").path("id").asInt()).get());
                    if(incident.path("assist1").path("id")!=null && playerRepository.findById(incident.path("assist1").path("id").asInt()).isPresent())
                    {
                        matchIncident.setRelatedPlayer(playerRepository.findById(incident.path("assist1").path("id").asInt()).get());
                    }
                    matchIncident.setHomeScore(incident.path("homeScore").asInt());
                    matchIncident.setAwayScore(incident.path("awayScore").asInt());
                    matchIncident.setTeam(incident.path("isHome").asBoolean()?homeTeam:awayTeam);
                    break;
                case "substitution" :
                    matchIncident.setType("Substitution");
                    matchIncident.setMatch(match);
                    matchIncident.setMinute(incident.path("time").asInt());
                    matchIncident.setText(incident.path("incidentClass").asText());
                    matchIncident.setPlayer(playerRepository.findById(incident.path("playerIn").path("id").asInt()).get());
                    matchIncident.setRelatedPlayer(playerRepository.findById(incident.path("playerOut").path("id").asInt()).get());
                    matchIncident.setTeam(incident.path("isHome").asBoolean()?homeTeam:awayTeam);
                    break;
                case "card":
                    matchIncident.setType("Card");
                    matchIncident.setMatch(match);
                    matchIncident.setMinute(incident.path("time").asInt());
                    if(playerRepository.findById(incident.path("player").path("id").asInt()).isPresent())
                    {
                        matchIncident.setPlayer(playerRepository.findById(incident.path("player").path("id").asInt()).get());
                    }
                    matchIncident.setTeam(incident.path("isHome").asBoolean()?homeTeam:awayTeam);
                    matchIncident.setText(incident.path("reason").asText());
                    matchIncident.setCardType(incident.path("incidentClass").asText());
                    break;
                case "default":
                    log.info("Skipping event");
            }
            kafkaProducer.sendEvent(new KafkaMatchEvent("MATCH_INCIDENT",matchId,matchIncident));
        }
    }

    private void processPlayerMatchStatistics(JsonNode lineupData, Match match,int matchId) {
        for(JsonNode player : lineupData)
        {
            JsonNode statistics = player.path("statistics");
            PlayerMatchStatistics playerMatchStatistics = new PlayerMatchStatistics();
            playerMatchStatistics.setMatch(match);
            if(playerRepository.findById(player.path("player").path("id").asInt()).isEmpty())
            {
                continue;
            }
            playerMatchStatistics.setPlayer(playerRepository.findById(player.path("player").path("id").asInt()).get());
            playerMatchStatistics.setMinutesPlayed(statistics.path("minutesPlayed").asInt());
            playerMatchStatistics.setAssists(statistics.path("goalAssist").asInt());
            playerMatchStatistics.setPasses(statistics.path("totalPass").asInt());
            playerMatchStatistics.setTackles(statistics.path("totalTackle").asInt(0));
            playerMatchStatistics.setTouches(statistics.path("touches").asInt());
            playerMatchStatistics.setShots(statistics.path("totalShots").asInt());
            playerMatchStatistics.setSaves(statistics.path("saves").asInt());
            playerMatchStatistics.setGoalsPrevented(statistics.path("goalsPrevented").asInt());
            playerMatchStatistics.setSubstitute(player.path("substitute").asBoolean());
            playerMatchStatistics.setGoals(0);
            playerMatchStatistics.setYellowCards(0);
            playerMatchStatistics.setRedCards(0);
            kafkaProducer.sendEvent(new KafkaMatchEvent("PLAYER_MATCH_STATISTICS",matchId,playerMatchStatistics));
        }

    }

    private void processTeamMatchStatistics(JsonNode matchData,JsonNode statisticsData,int matchId) throws Exception {
        TeamMatchStatistics homeTeamMatchStatistics = new TeamMatchStatistics();
        TeamMatchStatistics awayTeamMatchStatistics = new TeamMatchStatistics();
        int homeTeamId = matchData.path("event").path("homeTeam").path("id").asInt();
        int awayTeamId = matchData.path("event").path("awayTeam").path("id").asInt();
        Team homeTeam = teamRepository.findById(homeTeamId).get();
        Team awayTeam = teamRepository.findById(awayTeamId).get();
        homeTeamMatchStatistics.setMatch(matchRepository.findById(matchId).get());
        homeTeamMatchStatistics.setTeam(homeTeam);
        homeTeamMatchStatistics.setPeriod("ALL");
        awayTeamMatchStatistics.setMatch(matchRepository.findById(matchId).get());
        awayTeamMatchStatistics.setTeam(awayTeam);
        awayTeamMatchStatistics.setPeriod("ALL");
        Map<String, Map<String, JsonNode>> statsMap = new HashMap<>();
        for (JsonNode group : statisticsData) {
            for (JsonNode item : group.path("statisticsItems")) {
                statsMap.put(item.path("key").asText(), Map.of("h", item.path("homeValue"), "a", item.path("awayValue")));
            }
        }
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(statsMap));
        populateTeamStats(homeTeamMatchStatistics, statsMap, "h");
        populateTeamStats(awayTeamMatchStatistics, statsMap, "a");
        kafkaProducer.sendEvent(new KafkaMatchEvent("TEAM_MATCH_STATISTICS",matchId,homeTeamMatchStatistics));
        kafkaProducer.sendEvent(new KafkaMatchEvent("TEAM_MATCH_STATISTICS",matchId,awayTeamMatchStatistics));
    }

    private void processLeagueStandings(JsonNode matchData,Page page,int matchId) throws Exception {
        Match match = matchRepository.findById(matchId).get();
        int homeTeamId = matchData.path("event").path("homeTeam").path("id").asInt();
        int awayTeamId = matchData.path("event").path("awayTeam").path("id").asInt();
        Team homeTeam = teamRepository.findById(homeTeamId).get();
        Team awayTeam = teamRepository.findById(awayTeamId).get();
        int round = matchData.path("event").path("roundInfo").path("round").asInt();
        String status = matchData.path("event").path("status").path("type").asText();
        int homeScore = matchData.path("event").path("homeScore").path("current").asInt();
        int awayScore = matchData.path("event").path("awayScore").path("current").asInt();
        LeagueStandingsUpdation leagueStandingsUpdation = new LeagueStandingsUpdation(match,homeTeam,awayTeam,homeScore,awayScore,round,status);
        kafkaProducer.sendEvent(new KafkaMatchEvent("LEAGUE_STANDINGS_UPDATION",matchId,leagueStandingsUpdation));
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

    private JsonNode fetchJson(Page page, String url) throws Exception {
        Response response = page.navigate(url);
        if (response != null && response.status() == 200) {
            return mapper.readTree(response.text());
        }
        return mapper.createObjectNode();
    }
}