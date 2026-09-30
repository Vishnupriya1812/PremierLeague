package com.example.pl_core_data.model;

import com.example.pl_core_data.entity.Match;
import com.example.pl_core_data.entity.Team;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeagueStandingsUpdation {
    private Match match;
    private Team homeTeam;
    private Team awayTeam;
    private Integer homeScore;
    private Integer awayScore;
    private Integer round;
    private String status;
}
