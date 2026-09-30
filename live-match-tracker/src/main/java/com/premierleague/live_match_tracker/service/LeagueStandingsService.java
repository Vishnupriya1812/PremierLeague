package com.premierleague.live_match_tracker.service;

import com.example.pl_core_data.entity.LeagueStandings;
import com.example.pl_core_data.entity.Match;
import com.example.pl_core_data.entity.Season;
import com.example.pl_core_data.entity.Team;
import com.example.pl_core_data.repository.LeagueStandingsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LeagueStandingsService {

    private final LeagueStandingsRepository standingsRepository;

    @Transactional
    public void updateStandings(Match match, Team team, int goalsScored, int goalsConceded, int matchday, String status) {

        // 1. Find the record from the IMMEDIATE previous matchday
        LeagueStandings prev = standingsRepository
                .findFirstByTeamAndSeasonAndMatchdayLessThanOrderByMatchdayDesc(team, match.getSeason(), matchday)
                .orElseGet(() -> createInitialStanding(team, match.getSeason()));

        // 2. Find or create the current record
        LeagueStandings current = standingsRepository
                .findByTeamAndSeasonAndMatchday(team, match.getSeason(), matchday)
                .orElse(new LeagueStandings());

        current.setTeam(team);
        current.setSeason(match.getSeason());
        current.setMatchday(matchday);
        current.setStatus(status); // "in_progress" or "finished"

        // 3. Calculation Logic
        current.setPlayed(prev.getPlayed() + 1);
        current.setGoalsFor(prev.getGoalsFor() + goalsScored);
        current.setGoalsAgainst(prev.getGoalsAgainst() + goalsConceded);

        if (goalsScored > goalsConceded) { // WIN
            current.setWon(prev.getWon() + 1);
            current.setDrew(prev.getDrew());
            current.setLost(prev.getLost());
            current.setPoints(prev.getPoints() + 3);
        } else if (goalsScored == goalsConceded) { // DRAW
            current.setWon(prev.getWon());
            current.setDrew(prev.getDrew() + 1);
            current.setLost(prev.getLost());
            current.setPoints(prev.getPoints() + 1);
        } else { // LOSS
            current.setWon(prev.getWon());
            current.setDrew(prev.getDrew());
            current.setLost(prev.getLost() + 1);
            current.setPoints(prev.getPoints());
        }

        standingsRepository.save(current);
    }

    private LeagueStandings createInitialStanding(Team team, Season season) {
        LeagueStandings s = new LeagueStandings();
        s.setTeam(team);
        s.setSeason(season);
        s.setPlayed(0); s.setWon(0); s.setDrew(0); s.setLost(0);
        s.setGoalsFor(0); s.setGoalsAgainst(0); s.setPoints(0);
        return s;
    }
}