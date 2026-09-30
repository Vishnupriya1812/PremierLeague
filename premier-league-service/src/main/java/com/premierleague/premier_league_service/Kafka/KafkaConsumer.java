package com.premierleague.premier_league_service.Kafka;

import com.example.pl_core_data.entity.*;
import com.example.pl_core_data.model.KafkaMatchEvent;
import com.example.pl_core_data.model.LeagueStandingsUpdation;
import com.example.pl_core_data.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.premierleague.premier_league_service.service.LeagueStandingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaConsumer {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MatchLineupRepository matchLineupRepository;
    private final MatchIncidentRepository matchIncidentRepository;
    private final PlayerMatchStatisticsRepository playerMatchStatisticsRepository;
    private final TeamMatchStatisticsRepository teamMatchStatisticsRepository;
    private final LeagueStandingsService leagueStandingsService;
    private final MatchRepository matchRepository;

    public KafkaConsumer( MatchLineupRepository matchLineupRepository, MatchRepository matchRepository,MatchIncidentRepository matchIncidentRepository, PlayerMatchStatisticsRepository playerMatchStatisticsRepository, TeamMatchStatisticsRepository teamMatchStatisticsRepository, LeagueStandingsService leagueStandingsService) {

        this.matchLineupRepository = matchLineupRepository;
        this.matchIncidentRepository = matchIncidentRepository;
        this.playerMatchStatisticsRepository = playerMatchStatisticsRepository;
        this.teamMatchStatisticsRepository = teamMatchStatisticsRepository;
        this.leagueStandingsService = leagueStandingsService;
        this.matchRepository = matchRepository;
    }

    @KafkaListener(topics = "match.events", groupId = "pl-ingestion-group")
    public void handleMatchEvent(KafkaMatchEvent event) throws JsonProcessingException {
        String payload = objectMapper.writeValueAsString(event.getPayload());
        processMatchEvent(event.getEventType(),event.getMatchId(),payload);
    }

    public void processMatchEvent(String eventType,int matchId,String payload) throws JsonProcessingException {
        switch(eventType) {
            case "MATCH_DETAILS":
                Match match = objectMapper.readValue(payload, Match.class);
                log.info("\nReceived KafkaMatchEvent \nEvent Type: {}\nMatch Id : {}\nMatch Object{}", eventType, matchId,match);
                log.info("Saving match data: {} vs {}", match.getHomeTeam().getName(),match.getAwayTeam().getName());
                log.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(match));
                log.info("----------------------------------------");
                log.info("Persisting match data to the database {}",match.getId());
                matchRepository.upsertMatch(match);
                break;
            case "MATCH_LINEUP_ENTRY":
                MatchLineup matchLineup = objectMapper.readValue(payload, MatchLineup.class);
                log.info("\nReceived KafkaMatchEvent \nEvent Type: {}\nMatch Id : {}\nMatch  Line Up Object\n{}", eventType , matchId,matchLineup);
                log.info("----------------------------------------");
                log.info("Persisting match lineup data to the database {}",matchLineup.getId());
                matchLineupRepository.upsertLineup(matchLineup);
                break;
            case "MATCH_INCIDENT":
                MatchIncident matchIncident = objectMapper.readValue(payload, MatchIncident.class);
                log.info("\nReceived KafkaMatchEvent \nEvent Type: {}\nMatch Id : {}\nMatch  Incident Object\n{}", eventType, matchId,matchIncident);
                log.info("----------------------------------------");
                log.info("Persisting match incident data to the database {}",matchIncident.getId());
                matchIncidentRepository.upsertIncident(matchIncident);
                playerMatchStatisticsRepository.syncIncidentsToStats(matchId);
                break;
            case "PLAYER_MATCH_STATISTICS":
                PlayerMatchStatistics playerMatchStatistics = objectMapper.readValue(payload, PlayerMatchStatistics.class);
                log.info("\nReceived KafkaMatchEvent \nEvent Type: {}\nMatch Id : {}\nPlayer Match Statistics Object\n{}", eventType, matchId,playerMatchStatistics);
                log.info("----------------------------------------");
                log.info("Persisting player stats to the database {}",playerMatchStatistics.getId());
                playerMatchStatisticsRepository.upsertPlayerStats(playerMatchStatistics);
                break;
            case "TEAM_MATCH_STATISTICS":
                TeamMatchStatistics teamMatchStatistics = objectMapper.readValue(payload, TeamMatchStatistics.class);
                log.info("\nReceived KafkaMatchEvent \nEvent Type: {}\nMatch Id : {}\nTeam Match Statistics Object\n{}", eventType, matchId,teamMatchStatistics);
                log.info("----------------------------------------");
                log.info("Persisting team stats to the database {}",teamMatchStatistics.getId());
                teamMatchStatisticsRepository.upsertTeamStats(teamMatchStatistics);
                break;
            case "LEAGUE_STANDINGS_UPDATION":
                LeagueStandingsUpdation leagueStandingsUpdation = objectMapper.readValue(payload, LeagueStandingsUpdation.class);
                log.info("\nReceived KafkaMatchEvent \nEvent Type: {}\nMatch Id : {}\nLeague Standings Updation Object\n{}", eventType, matchId,leagueStandingsUpdation);
                Match match_to_update = leagueStandingsUpdation.getMatch();
                Team homeTeam = leagueStandingsUpdation.getHomeTeam();
                Team awayTeam = leagueStandingsUpdation.getAwayTeam();
                int homeGoalsScored = leagueStandingsUpdation.getHomeScore();
                int awayGoalsScored = leagueStandingsUpdation.getAwayScore();
                int matchDay = leagueStandingsUpdation.getRound();
                String status = leagueStandingsUpdation.getStatus();
                log.info("----------------------------------------");
                log.info("Persisting standings data to the database {}",leagueStandingsUpdation.getRound());
                leagueStandingsService.updateStandings(match_to_update,homeTeam,homeGoalsScored,awayGoalsScored,matchDay,status);
                log.info("Persisting standings data to the database {}",leagueStandingsUpdation.getRound());
                leagueStandingsService.updateStandings(match_to_update,awayTeam,awayGoalsScored,homeGoalsScored,matchDay,status);
                break;
            default:
                log.warn("Unknown event type: {}, match id : {}", eventType,matchId);
        }
    }
}
