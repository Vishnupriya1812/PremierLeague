package com.premierleague.premier_league_service.dto;

public record MatchSummaryResponse(
        Integer matchId,
        String homeTeam,
        String awayTeam,
        Integer homeScore,
        Integer awayScore,
        String summary
) {}
