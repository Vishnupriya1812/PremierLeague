package com.premierleague.premier_league_service.controller;

import com.example.pl_core_data.DTO.MatchLineupDTO;
import com.example.pl_core_data.DTO.MatchesDTO;
import com.example.pl_core_data.DTO.StandingsDTO;
import com.example.pl_core_data.DTO.TopPerformersDTO;
import com.example.pl_core_data.repository.LeagueStandingsRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "League Analytics", description = "Endpoints for standings and performance data")
public class premierLeagueController
{
    private final LeagueStandingsRepository leagueStandingsRepository;

    public premierLeagueController(LeagueStandingsRepository leagueStandingsRepository) {

        this.leagueStandingsRepository = leagueStandingsRepository;
    }

    @Operation(summary = "Get current standings", description = "Returns the live points table for a specific season")
    @GetMapping("/api/premier-league/home/standings")
    public ResponseEntity<List<StandingsDTO>> getStandings(@RequestParam Integer seasonId)
    {
        return ResponseEntity.ok(leagueStandingsRepository.findCurrentPointsTable(76986));
    }

    @Operation(summary = "Get standings by round", description = "Returns the league standings for a specific round in the season")
    @GetMapping("/api/premier-league/home/standingsByRound")
    public ResponseEntity<List<StandingsDTO>> getStandingsBySeasonRound(@RequestParam Integer round)
    {
        return ResponseEntity.ok(leagueStandingsRepository.findStandingsBySeasonAndRound(76986,round));
    }

    @Operation(summary = "Get matches by round", description = "Returns all matches played in a specific round")
    @GetMapping("/api/premier-league/home/matchesByRound")
    public ResponseEntity<List<MatchesDTO>> getMatchesByRound(@RequestParam Integer round)
    {
        return ResponseEntity.ok(leagueStandingsRepository.findMatchesByRound(76986,round));
    }

    @Operation(summary = "Get top eleven players", description = "Returns the top performing players for a specific round")
    @GetMapping("/api/premier-league/home/topEleven")
    public ResponseEntity<List<MatchLineupDTO>> getTopPlayers(@RequestParam Integer round)
    {
        return ResponseEntity.ok(leagueStandingsRepository.findTopPerformersPool(76986,round));
    }

    @Operation(summary = "Get top performers by statistic", description = "Returns top performers based on a specific statistic column")
    @GetMapping("/api/premier-league/home/topPerformers")
    public ResponseEntity<List<TopPerformersDTO>> getTopPerformers(@RequestParam String column)
    {
        return ResponseEntity.ok(leagueStandingsRepository.findTopPerformersByStat(76986,column));
    }

    //Match

}
