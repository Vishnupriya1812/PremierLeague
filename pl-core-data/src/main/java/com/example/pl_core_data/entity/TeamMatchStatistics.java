package com.example.pl_core_data.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "team_match_statistics", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"match_id", "team_id", "period"})
})
public class TeamMatchStatistics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "match_id")
    private Match match;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    private String period; // "ALL"

    // --- High Value Core Stats ---
    private Integer ballPossession;
    private Double expectedGoals;
    private Integer bigChanceCreated;
    private Integer bigChanceMissed;
    private Integer shotsOnGoal;
    private Integer shotsOffGoal;
    private Integer cornerKicks;
    private Integer fouls;
    private Integer offsides;
    private Integer yellowCards;
    private Integer passes; // Total passes is usually enough
}