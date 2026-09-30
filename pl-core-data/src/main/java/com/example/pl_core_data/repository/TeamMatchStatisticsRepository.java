package com.example.pl_core_data.repository;

import com.example.pl_core_data.entity.TeamMatchStatistics;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMatchStatisticsRepository extends JpaRepository<TeamMatchStatistics, Long> {

    /**
     * Finds the statistics for both teams in a specific match.
     * Usually returns 2 records (Home and Away) for the "ALL" period.
     */
    List<TeamMatchStatistics> findByMatchId(Integer matchId);

    /**
     * Finds statistics for a specific team in a specific match for a specific period.
     */
    Optional<TeamMatchStatistics> findByMatchIdAndTeamIdAndPeriod(Integer matchId, Integer teamId, String period);

    /**
     * Finds all statistics for a team across the entire season.
     * Useful for calculating averages (e.g., average possession).
     */
    List<TeamMatchStatistics> findByTeamIdAndPeriod(Integer teamId, String period);

    /**
     * Example of a custom query to find high-performance matches:
     * Teams that had more than 60% possession but lost the match.
     */
    @Query("SELECT t FROM TeamMatchStatistics t WHERE t.ballPossession > 60 AND t.period = 'ALL'")
    List<TeamMatchStatistics> findHighPossessionGames();

    @Modifying
    @Transactional
    @Query(value = """
    INSERT INTO team_match_statistics (
        match_id, team_id, period, ball_possession, expected_goals, 
        big_chance_created, big_chance_missed,
        shots_on_goal, shots_off_goal, corner_kicks, fouls, 
        offsides, yellow_cards, passes
    )
    VALUES (
        :#{#s.match?.id}, :#{#s.team?.id}, :#{#s.period}, :#{#s.ballPossession}, 
        :#{#s.expectedGoals}, :#{#s.bigChanceCreated}, :#{#s.bigChanceMissed}, 
         :#{#s.shotsOnGoal}, :#{#s.shotsOffGoal}, 
        :#{#s.cornerKicks}, :#{#s.fouls}, :#{#s.offsides}, :#{#s.yellowCards}, :#{#s.passes}
    )
    ON CONFLICT (match_id, team_id, period) DO UPDATE SET
        ball_possession = EXCLUDED.ball_possession,
        expected_goals = EXCLUDED.expected_goals,
        big_chance_created = EXCLUDED.big_chance_created,
        big_chance_missed = EXCLUDED.big_chance_missed,
        shots_on_goal = EXCLUDED.shots_on_goal,
        shots_off_goal = EXCLUDED.shots_off_goal,
        corner_kicks = EXCLUDED.corner_kicks,
        fouls = EXCLUDED.fouls,
        offsides = EXCLUDED.offsides,
        yellow_cards = EXCLUDED.yellow_cards,
        passes = EXCLUDED.passes
""", nativeQuery = true)
    void upsertTeamStats(@Param("s") TeamMatchStatistics stats);
}