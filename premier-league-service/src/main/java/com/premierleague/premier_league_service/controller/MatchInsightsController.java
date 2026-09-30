package com.premierleague.premier_league_service.controller;

import com.premierleague.premier_league_service.dto.MatchSummaryResponse;
import com.premierleague.premier_league_service.service.MatchInsightsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;

@RestController
@Tag(name = "AI Match Insights", description = "LLM-generated natural-language match summaries (Google Gemini)")
public class MatchInsightsController {

    private final MatchInsightsService matchInsightsService;

    public MatchInsightsController(MatchInsightsService matchInsightsService) {
        this.matchInsightsService = matchInsightsService;
    }

    @Operation(
            summary = "Get an AI-generated match summary",
            description = "Builds a natural-language recap of the match from stored score, team stats, " +
                    "incidents, and top performers, using Google's Gemini API."
    )
    @GetMapping("/api/premier-league/matches/{matchId}/summary")
    public ResponseEntity<MatchSummaryResponse> getMatchSummary(@PathVariable Integer matchId) {
        try {
            return ResponseEntity.ok(matchInsightsService.generateSummary(matchId));
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        }
    }
}
