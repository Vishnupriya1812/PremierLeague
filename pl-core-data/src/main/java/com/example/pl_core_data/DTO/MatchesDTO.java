package com.example.pl_core_data.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchesDTO {
    private Integer id;
    private Integer attendance;
    private Integer awayScore;
    private Integer homeScore;
    private Integer htAwayScore;
    private Integer htHomeScore;
    private Long kickoffTime;
    private String referee;
    private Integer round;
    private String status;
    private String venue;
    private String homeTeam;
    private String awayTeam;
    private String season;
    private Double minute;
}