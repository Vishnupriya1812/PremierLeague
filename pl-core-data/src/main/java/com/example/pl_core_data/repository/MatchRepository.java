package com.example.pl_core_data.repository;

import com.example.pl_core_data.entity.Match;
import com.example.pl_core_data.entity.Season;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Integer> {
    // Find all matches for a specific season if you need to perform analytics later
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO matches (
            id, home_score, away_score, ht_home_score, ht_away_score, 
            attendance, venue, referee, status, kickoff_time, 
            home_team_id, away_team_id, season_id, round, minute
        ) VALUES (
            :#{#m.id}, :#{#m.homeScore}, :#{#m.awayScore}, :#{#m.htHomeScore}, :#{#m.htAwayScore}, 
            :#{#m.attendance}, :#{#m.venue}, :#{#m.referee}, :#{#m.status}, :#{#m.kickoffTime}, 
            :#{#m.homeTeam?.id}, :#{#m.awayTeam?.id}, :#{#m.season?.id}, :#{#m.round}, :#{#m.minute}
        ) 
        ON CONFLICT (id) DO UPDATE SET 
            home_score = EXCLUDED.home_score,
            away_score = EXCLUDED.away_score,
            ht_home_score = EXCLUDED.ht_home_score,
            ht_away_score = EXCLUDED.ht_away_score,
            attendance = EXCLUDED.attendance,
            status = EXCLUDED.status,
            minute = EXCLUDED.minute
    """, nativeQuery = true)
    void upsertMatch(@Param("m") Match match);
    List<Match> findBySeason(Season season);
    boolean existsById(Integer id);
    Optional<Match> findById(Integer id);

    @Query("SELECT m.id FROM Match m WHERE m.round = :round AND m.season.id = :seasonId")
    List<Integer> findMatchIdsByRoundAndSeasonId(@Param("round") Integer round,
                                                 @Param("seasonId") Integer seasonId);
}
