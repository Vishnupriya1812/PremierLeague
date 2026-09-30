package com.example.pl_core_data.repository;

import com.example.pl_core_data.entity.MatchIncident;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchIncidentRepository extends JpaRepository<MatchIncident, Long> {

    boolean existsByMatchIdAndPlayerIdAndTypeAndMinute(
            Integer matchId,
            Integer playerId,
            String type,
            Integer minute
    );
    /**
     * Finds all incidents for a specific match, ordered by time.
     * Useful for building a "Match Timeline".
     */
    List<MatchIncident> findByMatchIdOrderByMinuteAsc(Integer matchId);

    /**
     * Finds all goals or cards for a specific player across all matches.
     */
    List<MatchIncident> findByPlayerIdAndType(Integer playerId, String type);

    /**
     * Critical for Idempotency:
     * Checks if a specific incident (e.g., a yellow card at the 42nd minute) already exists.
     */
    Optional<MatchIncident> findByMatchIdAndPlayerIdAndTypeAndMinute(
            Integer matchId,
            Integer playerId,
            String type,
            Integer minute
    );

    /**
     * Finds all incidents for a specific team in a match.
     */
    List<MatchIncident> findByMatchIdAndTeamId(Integer matchId, Integer teamId);
    @Modifying
    @Transactional
    @Query(value = """
    INSERT INTO match_incidents (
        match_id, player_id, team_id, type, minute, card_type, 
        related_player_id, text, home_score, away_score
    )
    VALUES (
        :#{#m.match?.id}, 
        :#{#m.player?.id}, 
        :#{#m.team?.id}, 
        :#{#m.type}, 
        :#{#m.minute}, 
        :#{#m.cardType}, 
        :#{#m.relatedPlayer?.id},
        :#{#m.text},
        :#{#m.homeScore},
        :#{#m.awayScore}
    )
    -- DO NOT use ON CONSTRAINT here. Use the index expression:
    ON CONFLICT (match_id, (COALESCE(player_id, -1)), type, minute) 
    DO UPDATE SET
        home_score = EXCLUDED.home_score,
        away_score = EXCLUDED.away_score,
        text = EXCLUDED.text
""", nativeQuery = true)
    void upsertIncident(@Param("m") MatchIncident matchIncident);
}