package com.premierleague.premier_league_service.service;

import com.example.pl_core_data.entity.*;
import com.example.pl_core_data.repository.*;
import com.premierleague.premier_league_service.ai.GeminiClient;
import com.premierleague.premier_league_service.dto.MatchSummaryResponse;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Builds a natural-language match recap by feeding the aggregated match data
 * we already store (score, team stats, incidents, lineups) to Gemini.
 */
@Service
public class MatchInsightsService {

    private final MatchRepository matchRepository;
    private final TeamMatchStatisticsRepository teamMatchStatisticsRepository;
    private final MatchIncidentRepository matchIncidentRepository;
    private final MatchLineupRepository matchLineupRepository;
    private final GeminiClient geminiClient;

    public MatchInsightsService(
            MatchRepository matchRepository,
            TeamMatchStatisticsRepository teamMatchStatisticsRepository,
            MatchIncidentRepository matchIncidentRepository,
            MatchLineupRepository matchLineupRepository,
            GeminiClient geminiClient
    ) {
        this.matchRepository = matchRepository;
        this.teamMatchStatisticsRepository = teamMatchStatisticsRepository;
        this.matchIncidentRepository = matchIncidentRepository;
        this.matchLineupRepository = matchLineupRepository;
        this.geminiClient = geminiClient;
    }

    public MatchSummaryResponse generateSummary(Integer matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new NoSuchElementException("No match found with id " + matchId));

        String prompt = buildPrompt(match);
        String summary = geminiClient.generateText(prompt).trim();

        return new MatchSummaryResponse(
                match.getId(),
                match.getHomeTeam() != null ? match.getHomeTeam().getName() : null,
                match.getAwayTeam() != null ? match.getAwayTeam().getName() : null,
                match.getHomeScore(),
                match.getAwayScore(),
                summary
        );
    }

    private String buildPrompt(Match match) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a football (soccer) journalist. Write a concise, engaging match report ")
          .append("(150-220 words) using ONLY the facts given below. Do not invent players, stats, ")
          .append("or events that aren't listed. If data is missing, simply omit it.\n\n");

        String home = match.getHomeTeam() != null ? match.getHomeTeam().getName() : "Home";
        String away = match.getAwayTeam() != null ? match.getAwayTeam().getName() : "Away";

        sb.append("MATCH: ").append(home).append(" vs ").append(away).append("\n");
        sb.append("FINAL SCORE: ").append(nullSafe(match.getHomeScore())).append(" - ")
          .append(nullSafe(match.getAwayScore())).append("\n");
        if (match.getHtHomeScore() != null && match.getHtAwayScore() != null) {
            sb.append("HALF-TIME SCORE: ").append(match.getHtHomeScore()).append(" - ")
              .append(match.getHtAwayScore()).append("\n");
        }
        if (match.getVenue() != null) sb.append("VENUE: ").append(match.getVenue()).append("\n");
        if (match.getReferee() != null) sb.append("REFEREE: ").append(match.getReferee()).append("\n");
        if (match.getAttendance() != null) sb.append("ATTENDANCE: ").append(match.getAttendance()).append("\n");

        List<TeamMatchStatistics> teamStats = teamMatchStatisticsRepository.findByMatchId(match.getId());
        if (!teamStats.isEmpty()) {
            sb.append("\nTEAM STATS:\n");
            for (TeamMatchStatistics s : teamStats) {
                String teamName = s.getTeam() != null ? s.getTeam().getName() : "Unknown";
                sb.append("- ").append(teamName).append(": ")
                  .append("possession=").append(nullSafe(s.getBallPossession())).append("%, ")
                  .append("xG=").append(nullSafe(s.getExpectedGoals())).append(", ")
                  .append("shots on target=").append(nullSafe(s.getShotsOnGoal())).append(", ")
                  .append("shots off target=").append(nullSafe(s.getShotsOffGoal())).append(", ")
                  .append("corners=").append(nullSafe(s.getCornerKicks())).append(", ")
                  .append("fouls=").append(nullSafe(s.getFouls())).append(", ")
                  .append("offsides=").append(nullSafe(s.getOffsides())).append(", ")
                  .append("yellow cards=").append(nullSafe(s.getYellowCards())).append("\n");
            }
        }

        List<MatchIncident> incidents = matchIncidentRepository.findByMatchIdOrderByMinuteAsc(match.getId());
        if (!incidents.isEmpty()) {
            sb.append("\nKEY INCIDENTS (chronological):\n");
            for (MatchIncident inc : incidents) {
                sb.append("- ");
                if (inc.getMinute() != null) sb.append(inc.getMinute()).append("': ");
                sb.append(inc.getType());
                if (inc.getPlayer() != null) sb.append(" — ").append(inc.getPlayer().getName());
                if (inc.getTeam() != null) sb.append(" (").append(inc.getTeam().getName()).append(")");
                if (inc.getCardType() != null) sb.append(" [").append(inc.getCardType()).append(" card]");
                if (inc.getText() != null && !inc.getText().isBlank()) sb.append(" - ").append(inc.getText());
                sb.append("\n");
            }
        }

        List<MatchLineup> lineups = matchLineupRepository.findByMatchId(match.getId());
        if (!lineups.isEmpty()) {
            List<MatchLineup> topRated = lineups.stream()
                    .filter(l -> l.getRating() != null)
                    .sorted(Comparator.comparing(MatchLineup::getRating).reversed())
                    .limit(3)
                    .toList();
            if (!topRated.isEmpty()) {
                sb.append("\nTOP PERFORMERS (by match rating):\n");
                for (MatchLineup l : topRated) {
                    String playerName = l.getPlayer() != null ? l.getPlayer().getName() : "Unknown";
                    String teamName = l.getTeam() != null ? l.getTeam().getName() : "Unknown";
                    sb.append("- ").append(playerName).append(" (").append(teamName).append("): ")
                      .append(l.getRating()).append("\n");
                }
            }
        }

        return sb.toString();
    }

    private String nullSafe(Object value) {
        return value == null ? "N/A" : value.toString();
    }
}
