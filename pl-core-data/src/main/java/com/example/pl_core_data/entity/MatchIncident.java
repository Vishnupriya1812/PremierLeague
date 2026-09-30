package com.example.pl_core_data.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "match_incidents", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_match_player_incident_time",
                columnNames = {"match_id", "player_id", "incident_type", "time"}
        )
})
@Data
public class MatchIncident {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "match_id")
    private Match match;

    @ManyToOne
    @JoinColumn(name = "player_id" ,nullable = true)
    private Player player;

    @ManyToOne
    @JoinColumn(name = "team_id",nullable = true)
    private Team team;

    // Use @Column to map the Java field to the specific DB column name
    @Column(name = "type")
    private String type;

    @Column(name = "minute")
    private Integer minute;

    @Column(name = "card_type",nullable = true)
    private String cardType;

    @ManyToOne
    @JoinColumn(name = "related_player_id",nullable = true)
    private Player relatedPlayer;

    @Column(name = "text",nullable = true)
    private String text;

    @Column(name = "homeScore",nullable = true)
    private int homeScore;

    @Column(name = "awayScore",nullable = true)
    private int awayScore;

}