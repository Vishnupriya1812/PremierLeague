package com.premier_league.data_ingestion_service.service;

//import com.premier_league.data_ingestion_service.entity.LeagueStandings;
//import com.premier_league.data_ingestion_service.entity.Match;
//import com.premier_league.data_ingestion_service.entity.Season;
//import com.premier_league.data_ingestion_service.entity.Team;
//import com.premier_league.data_ingestion_service.repository.LeagueStandingsRepository;

import com.example.pl_core_data.entity.*;
import com.example.pl_core_data.repository.*;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class LeagueStandingsService {

    private final LeagueStandingsRepository standingsRepository;

    @Transactional // Ensures atomic updates
    public void updateStandings(Match match, Team team, int goalsScored, int goalsConceded, int matchday) {

        // Find existing record for this matchday to support updates/idempotency
        LeagueStandings current = standingsRepository
                .findByTeamAndSeasonAndMatchday(team, match.getSeason(), matchday)
                .orElse(new LeagueStandings());

        // Find the record from the IMMEDIATE previous matchday to get cumulative stats
        LeagueStandings prev = standingsRepository
                .findFirstByTeamAndSeasonAndMatchdayLessThanOrderByMatchdayDesc(team, match.getSeason(), matchday)
                .orElseGet(() -> createInitialStanding(team, match.getSeason()));

        current.setTeam(team);
        current.setSeason(match.getSeason());
        current.setMatchday(matchday);

        // Cumulative Calculations
        current.setPlayed(prev.getPlayed() + 1);
        current.setGoalsFor(prev.getGoalsFor() + goalsScored);
        current.setGoalsAgainst(prev.getGoalsAgainst() + goalsConceded);

        // Result Logic
        if (goalsScored > goalsConceded) { // Win
            current.setWon(prev.getWon() + 1);
            current.setDrew(prev.getDrew());
            current.setLost(prev.getLost());
            current.setPoints(prev.getPoints() + 3);
        } else if (goalsScored == goalsConceded) { // Draw
            current.setWon(prev.getWon());
            current.setDrew(prev.getDrew() + 1);
            current.setLost(prev.getLost());
            current.setPoints(prev.getPoints() + 1);
        } else { // Loss
            current.setWon(prev.getWon());
            current.setDrew(prev.getDrew());
            current.setLost(prev.getLost() + 1);
            current.setPoints(prev.getPoints());
        }

        standingsRepository.save(current);
    }

    private LeagueStandings createInitialStanding(Team team, Season season) {
        LeagueStandings s = new LeagueStandings();
        s.setPlayed(0); s.setWon(0); s.setDrew(0); s.setLost(0);
        s.setGoalsFor(0); s.setGoalsAgainst(0); s.setPoints(0);
        return s;
    }
}