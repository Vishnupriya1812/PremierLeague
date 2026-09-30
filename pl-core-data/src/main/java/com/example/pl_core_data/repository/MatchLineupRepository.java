package com.example.pl_core_data.repository;
import com.example.pl_core_data.entity.Match;
import com.example.pl_core_data.entity.MatchLineup;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchLineupRepository extends JpaRepository<MatchLineup, Long> {
    boolean existsByMatchIdAndPlayerId(Integer matchId, Integer playerId);
    /**
     * Finds all lineup entries (players) for a specific match.
     * Useful for building the full match report.
     */
    List<MatchLineup> findByMatchId(Integer matchId);

    /**
     * Finds the lineup for a specific team within a specific match.
     * Use this to separate the Home XI from the Away XI.
     */
    List<MatchLineup> findByMatchIdAndTeamId(Integer matchId, Integer teamId);

    /**
     * Finds all matches a specific player has participated in.
     * Useful for player profile pages.
     */
    List<MatchLineup> findByPlayerId(Integer playerId);

    @Modifying
    @Transactional
    @Query(value = """
    INSERT INTO match_lineups (match_id, player_id, team_id, is_starting, position, shirt_number, captain, rating)
    VALUES (:#{#m.match.id}, :#{#m.player.id}, :#{#m.team.id}, :#{#m.isStarting}, :#{#m.position}, :#{#m.shirtNumber}, :#{#m.captain}, :#{#m.rating})
    ON CONFLICT (match_id, player_id) DO UPDATE SET
        is_starting = EXCLUDED.is_starting,
        captain = EXCLUDED.captain,
        rating = EXCLUDED.rating,
        position = EXCLUDED.position,
        shirt_number = EXCLUDED.shirt_number
""", nativeQuery = true)
    void upsertLineup(@Param("m") MatchLineup matchLineup);
}
