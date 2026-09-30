package com.example.pl_core_data.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Entity
@Table(name = "match_lineups", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"match_id", "player_id"})
})
@Data
@SqlResultSetMapping(
        name = "MatchLineupMapping",
        classes = @ConstructorResult(
                targetClass = com.example.pl_core_data.DTO.MatchLineupDTO.class,
                columns = {
                        @ColumnResult(name = "id",type = Integer.class),
                        @ColumnResult(name = "playerName", type = String.class),
                        @ColumnResult(name = "teamName", type = String.class),
                        @ColumnResult(name = "position", type = String.class),
                        @ColumnResult(name = "rating", type = Double.class)
                }
        )
)
public class MatchLineup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "match_id")
    private Match match;

    @ManyToOne
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne
    @JoinColumn(name = "team_id",nullable = false)
    private Team team;

    private Boolean isStarting;
    private String position;
    private Integer shirtNumber;
    private Double rating; // SofaScore Rating for Team of the Week
    private Boolean captain;
}
