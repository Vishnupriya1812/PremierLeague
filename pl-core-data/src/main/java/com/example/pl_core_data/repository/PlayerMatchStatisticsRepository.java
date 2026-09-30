package com.example.pl_core_data.repository;

import com.example.pl_core_data.entity.PlayerMatchStatistics;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerMatchStatisticsRepository extends JpaRepository<PlayerMatchStatistics, Long> {

    boolean existsByMatchIdAndPlayerId(Integer matchId, Integer playerId);
    /**
     * Finds performance stats for all players in a specific match.
     */
    List<PlayerMatchStatistics> findByMatchId(Integer matchId);

    /**
     * Find top performers by rating in a specific match.
     */
//    List<PlayerMatchStatistics> findByMatchIdOrderByRatingDesc(Integer matchId);

    /**
     * Find a specific player's stats for a specific match.
     */
    Optional<PlayerMatchStatistics> findByMatchIdAndPlayerId(Integer matchId, Integer playerId);

    @Modifying
    @Transactional
    @Query(value = """
    INSERT INTO player_match_stats (
        match_id, player_id, minutes_played, goals, assists, yellow_cards, 
        red_cards, passes, tackles, touches, shots, saves, goals_prevented,substitute
    )
    VALUES (
        :#{#s.match.id}, :#{#s.player.id}, :#{#s.minutesPlayed}, :#{#s.goals}, 
        :#{#s.assists}, :#{#s.yellowCards}, :#{#s.redCards}, :#{#s.passes}, 
        :#{#s.tackles}, :#{#s.touches}, :#{#s.shots}, :#{#s.saves}, :#{#s.goalsPrevented},:#{#s.substitute}
    )
    ON CONFLICT (match_id, player_id) DO UPDATE SET
        minutes_played = EXCLUDED.minutes_played,
        goals = EXCLUDED.goals,
        assists = EXCLUDED.assists,
        yellow_cards = EXCLUDED.yellow_cards,
        red_cards = EXCLUDED.red_cards,
        passes = EXCLUDED.passes,
        tackles = EXCLUDED.tackles,
        touches = EXCLUDED.touches,
        shots = EXCLUDED.shots,
        saves = EXCLUDED.saves,
        goals_prevented = EXCLUDED.goals_prevented,
        substitute = EXCLUDED.substitute
""", nativeQuery = true)
    void upsertPlayerStats(@Param("s") PlayerMatchStatistics stats);

    @Modifying
    @Transactional
    @Query(value = """
    UPDATE player_match_stats pms
    SET 
        goals = (SELECT COUNT(*) FROM match_incidents mi WHERE mi.match_id = pms.match_id AND mi.player_id = pms.player_id AND mi.type = 'Goal !'),
        yellow_cards = (SELECT COUNT(*) FROM match_incidents mi WHERE mi.match_id = pms.match_id AND mi.player_id = pms.player_id AND mi.card_type = 'yellow'),
        red_cards = (SELECT COUNT(*) FROM match_incidents mi WHERE mi.match_id = pms.match_id AND mi.player_id = pms.player_id AND mi.card_type = 'red')
    WHERE pms.match_id = :matchId
""", nativeQuery = true)
    void syncIncidentsToStats(@Param("matchId") int matchId);
}