package com.example.pl_core_data.entity;

import lombok.Data;
import jakarta.persistence.*;

@Entity
@Table(name = "league_standings", uniqueConstraints = {
        @UniqueConstraint(name = "uk_team_season_matchday",
                columnNames = {"team_id", "season_id", "matchday"})
})
@SqlResultSetMapping(
        name = "StandingsMapping",
        classes = @ConstructorResult(
                targetClass = com.example.pl_core_data.DTO.StandingsDTO.class,
                columns = {
                        @ColumnResult(name = "id", type = Integer.class),
                        @ColumnResult(name = "name", type = String.class),
                        @ColumnResult(name = "played", type = Integer.class),
                        @ColumnResult(name = "won", type = Integer.class),
                        @ColumnResult(name = "drew", type = Integer.class),
                        @ColumnResult(name = "lost", type = Integer.class),
                        @ColumnResult(name = "gf", type = Integer.class),
                        @ColumnResult(name = "ga", type = Integer.class),
                        @ColumnResult(name = "gd", type = Integer.class),
                        @ColumnResult(name = "points", type = Integer.class)
                }
        )
)
@Data
public class LeagueStandings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "season_id")
    private Season season;
    private Integer matchday;
    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;
    private Integer played;
    private Integer won;
    private Integer drew;
    private Integer lost;
    private Integer goalsFor;
    private Integer goalsAgainst;
    private Integer points;
    private String status;
}
