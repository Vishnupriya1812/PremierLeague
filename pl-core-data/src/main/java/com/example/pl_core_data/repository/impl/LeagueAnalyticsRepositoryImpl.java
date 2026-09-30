package com.example.pl_core_data.repository.impl;

import com.example.pl_core_data.DTO.MatchLineupDTO;
import com.example.pl_core_data.DTO.MatchesDTO;
import com.example.pl_core_data.DTO.StandingsDTO;
import com.example.pl_core_data.DTO.TopPerformersDTO;
import com.example.pl_core_data.repository.LeagueAnalyticsRepository;
import com.example.pl_core_data.util.RepositoryUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("leagueStandingsRepositoryImpl")
public class LeagueAnalyticsRepositoryImpl implements LeagueAnalyticsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final RepositoryUtils repositoryUtils;

    public LeagueAnalyticsRepositoryImpl(RepositoryUtils repositoryUtils) {
        this.repositoryUtils = repositoryUtils;
    }

    @Override
    public List<StandingsDTO> findCurrentPointsTable(Integer seasonId) {

        String sql = """
            SELECT t.id,t.name, ls.played, ls.won, ls.drew, ls.lost, 
                   ls.goals_for as gf, ls.goals_against as ga, 
                   (ls.goals_for - ls.goals_against) as gd, ls.points 
            FROM league_standings ls 
            JOIN teams t ON ls.team_id = t.id 
            JOIN (
                SELECT team_id, MAX(matchday) as max_md 
                FROM league_standings 
                WHERE season_id = :seasonId 
                GROUP BY team_id
            ) curr ON ls.team_id = curr.team_id AND ls.matchday = curr.max_md 
            WHERE ls.season_id = :seasonId 
            ORDER BY ls.points DESC, gd DESC, gf DESC
            """;

        return entityManager.createNativeQuery(sql, "StandingsMapping")
                .setParameter("seasonId", seasonId)
                .getResultList();
    }
    @Override
    public List<StandingsDTO> findStandingsBySeasonAndRound(Integer seasonId, Integer round) {
        String sql = """
                select t.id,name,played,won,drew,lost,goals_for as gf,goals_against as ga,(goals_for-goals_against)as gd,points 
                from 
                league_standings ls 
                join 
                teams t 
                on 
                ls.team_id=t.id 
                where 
                season_id=:seasonId 
                and 
                matchday=:round 
                order by points desc,gd desc,gf desc
                """;

        return entityManager.createNativeQuery(sql, "StandingsMapping")
                .setParameter("seasonId", 76986)
                .setParameter("round", round)
                .getResultList();
    }

    @Override
    public List<MatchesDTO> findMatchesByRound(Integer seasonId, Integer round)
    {
        String sql="""
                select  
                        m.id as id,
                        m.attendance,
                        m.away_score as awayScore,
                        m.home_score as homeScore,
                        m.ht_away_score as htAwayScore,
                        m.ht_home_score as htHomeScore,
                        m.kickoff_time as kickoffTime,
                        m.referee as referee,
                        m.round as round,
                        m.status as status,
                        m.venue as venue,
                        ht.name as homeTeam,
                        at.name as awayTeam,
                        s.year as season,
                        m.minute as minute
                from matches m 
                join teams ht on m.home_team_id=ht.id 
                join teams at on m.away_team_id=at.id 
                join seasons s on m.season_id=s.id 
                where m.season_id=:seasonId
                and m.round=:round;
                """;
        return entityManager.createNativeQuery(sql, "MatchesMapping")
                .setParameter("seasonId", 76986)
                .setParameter("round", round)
                .getResultList();
    }

    @Override
    public List<MatchLineupDTO> findTopPerformersPool(Integer seasonId, Integer round) {
        // We wrap your UNION in a subquery so we can JOIN once at the end for efficiency
        String sql = """
        SELECT p.id,p.name as playerName, t.name as teamName, pool.position, pool.rating
        FROM (
            (SELECT id, player_id, team_id, position, rating FROM match_lineups 
             WHERE position='F' AND match_id IN (SELECT id FROM matches WHERE season_id=:seasonId AND round=:round) 
             ORDER BY rating DESC LIMIT 3)
            UNION ALL
            (SELECT id, player_id, team_id, position, rating FROM match_lineups 
             WHERE position='D' AND match_id IN (SELECT id FROM matches WHERE season_id=:seasonId AND round=:round) 
             ORDER BY rating DESC LIMIT 5)
            UNION ALL
            (SELECT id, player_id, team_id, position, rating FROM match_lineups 
             WHERE position='M' AND match_id IN (SELECT id FROM matches WHERE season_id=:seasonId AND round=:round) 
             ORDER BY rating DESC LIMIT 5)
            UNION ALL
            (SELECT id, player_id, team_id, position, rating FROM match_lineups 
             WHERE position='G' AND match_id IN (SELECT id FROM matches WHERE season_id=:seasonId AND round=:round) 
             ORDER BY rating DESC LIMIT 1)
        ) as pool
        JOIN players p ON pool.player_id = p.id
        JOIN teams t ON pool.team_id = t.id
        ORDER BY pool.position, pool.rating DESC
        """;
        List<MatchLineupDTO> topPlayers = entityManager.createNativeQuery(sql, "MatchLineupMapping")
                .setParameter("seasonId", seasonId)
                .setParameter("round", round)
                .getResultList();

        return repositoryUtils.getTeamOfTheWeek(topPlayers);
    }

    @Override
    public List<TopPerformersDTO> findTopPerformersByStat(Integer seasonId, String statColumn) {
        // We use String.format for the column name because SQL column names cannot be parameters
        String sql = String.format("""
        SELECT 
            p.id,
            p.name AS playerName, 
            t.name AS teamName, 
            CAST(SUM(pms.%s) AS INTEGER) AS totalValue
        FROM player_match_stats pms
        JOIN match_lineups ml ON pms.match_id = ml.match_id AND pms.player_id = ml.player_id
        JOIN players p ON pms.player_id = p.id
        JOIN teams t ON ml.team_id = t.id
        JOIN matches m ON pms.match_id = m.id
        WHERE m.season_id = :seasonId
        GROUP BY p.id, p.name, t.name
        HAVING SUM(pms.%s) > 0
        ORDER BY totalValue DESC
        LIMIT 20
        """, statColumn, statColumn);

        return entityManager.createNativeQuery(sql, "TopScorerMapping")
                .setParameter("seasonId", seasonId)
                .getResultList();
    }
}
