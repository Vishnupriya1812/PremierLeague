package com.example.pl_core_data.repository;

import com.example.pl_core_data.DTO.MatchLineupDTO;
import com.example.pl_core_data.DTO.MatchesDTO;
import com.example.pl_core_data.DTO.StandingsDTO;
import com.example.pl_core_data.DTO.TopPerformersDTO;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LeagueAnalyticsRepository {
    @Query(nativeQuery = true)
    List<StandingsDTO> findCurrentPointsTable(Integer seasonId);
    @Query(nativeQuery = true)
    List<StandingsDTO> findStandingsBySeasonAndRound(Integer seasonId,Integer round);
    @Query(nativeQuery = true)
    List<MatchesDTO> findMatchesByRound(Integer seasonId, Integer round);
    @Query(nativeQuery = true)
    List<MatchLineupDTO> findTopPerformersPool(Integer seasonId, Integer round);
    List<TopPerformersDTO> findTopPerformersByStat(Integer seasonId, String statColumn);
}
