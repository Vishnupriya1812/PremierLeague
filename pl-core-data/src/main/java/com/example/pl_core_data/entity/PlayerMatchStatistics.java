package com.example.pl_core_data.entity;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "player_match_stats", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"match_id", "player_id"})
})
@Data
public class PlayerMatchStatistics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "match_id")
    private Match match;
    @ManyToOne
    @JoinColumn(name = "player_id")
    private Player player;
    private Integer minutesPlayed;
    private Integer goals;
    private Integer assists;
    private Integer yellowCards;
    private Integer redCards;
    private Integer passes;
    private Integer tackles;
    private Integer touches;
    private Integer shots;
    private Integer saves;
    private Integer goalsPrevented;
    private boolean substitute;
}

