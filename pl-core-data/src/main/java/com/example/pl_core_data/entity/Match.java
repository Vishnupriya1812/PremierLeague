package com.example.pl_core_data.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "matches")
@Data
@SqlResultSetMapping(
        name = "MatchesMapping",
        classes = @ConstructorResult(
                targetClass = com.example.pl_core_data.DTO.MatchesDTO.class,
                columns = {
                        @ColumnResult(name = "id", type = Integer.class),
                        @ColumnResult(name = "attendance", type = Integer.class),
                        @ColumnResult(name = "awayScore", type = Integer.class),
                        @ColumnResult(name = "homeScore", type = Integer.class),
                        @ColumnResult(name = "htAwayScore", type = Integer.class),
                        @ColumnResult(name = "htHomeScore", type = Integer.class),
                        @ColumnResult(name = "kickoffTime", type = Long.class),
                        @ColumnResult(name = "referee", type = String.class),
                        @ColumnResult(name = "round", type = Integer.class),
                        @ColumnResult(name = "status", type = String.class),
                        @ColumnResult(name = "venue", type = String.class),
                        @ColumnResult(name = "homeTeam", type = String.class),
                        @ColumnResult(name = "awayTeam", type = String.class),
                        @ColumnResult(name = "season", type = String.class),
                        @ColumnResult(name = "minute", type = Double.class)
                }
        )
)
public class Match {
    @Id
    private Integer id;

    private Integer homeScore;
    private Integer awayScore;
    private Integer htHomeScore; // period1
    private Integer htAwayScore; // period1

    private Integer attendance;
    private String venue;
    private String referee;

    private String status;
    private Long kickoffTime; // from startTimestamp

    @ManyToOne
    private Team homeTeam;

    @ManyToOne
    private Team awayTeam;

    @ManyToOne
    private Season season;

    private Integer round;
    private Double minute;
}