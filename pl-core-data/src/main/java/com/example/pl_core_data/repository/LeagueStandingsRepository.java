package com.example.pl_core_data.repository;

import com.example.pl_core_data.entity.LeagueStandings;
import com.example.pl_core_data.entity.Season;
import com.example.pl_core_data.entity.Team;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LeagueStandingsRepository extends JpaRepository<LeagueStandings, Long>, LeagueAnalyticsRepository {

    Optional<LeagueStandings> findFirstByTeamAndSeasonOrderByMatchdayDesc(Team team, Season season);

    boolean existsByTeamAndSeasonAndMatchday(Team team, Season season, Integer matchday);

    Optional<LeagueStandings> findByTeamAndSeasonAndMatchday(Team team, Season season, int matchday);

    Optional<LeagueStandings> findFirstByTeamAndSeasonAndMatchdayLessThanOrderByMatchdayDesc(Team team, Season season, int matchday);

    @Modifying
    @Transactional
    @Query(value = """
    INSERT INTO league_standings (
        season_id, team_id, matchday, played, won, drew, lost, 
        goals_for, goals_against, points, status
    )
    VALUES (
        :#{#s.season?.id}, :#{#s.team?.id}, :#{#s.matchday}, :#{#s.played}, 
        :#{#s.won}, :#{#s.drew}, :#{#s.lost}, :#{#s.goalsFor}, 
        :#{#s.goalsAgainst}, :#{#s.points}, :#{#s.status}
    )
    ON CONFLICT (season_id, team_id, matchday) DO UPDATE SET
        played = EXCLUDED.played,
        won = EXCLUDED.won,
        drew = EXCLUDED.drew,
        lost = EXCLUDED.lost,
        goals_for = EXCLUDED.goals_for,
        goals_against = EXCLUDED.goals_against,
        points = EXCLUDED.points,
        status = EXCLUDED.status
""", nativeQuery = true)
    void upsertStanding(@Param("s") LeagueStandings standing);

}
