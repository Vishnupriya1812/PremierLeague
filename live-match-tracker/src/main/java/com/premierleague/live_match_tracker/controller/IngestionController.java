package com.premierleague.live_match_tracker.controller;

import com.premierleague.live_match_tracker.service.LiveMatchesJobManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/live_match_ingestion")
public class IngestionController {

    private final LiveMatchesJobManager jobManager;

    public IngestionController(LiveMatchesJobManager jobManager) {
        this.jobManager = jobManager;
    }

    @PostMapping("/start/{round}")
    public ResponseEntity<String> start(@PathVariable int round) {
        jobManager.startIngestion(round);
        return ResponseEntity.ok("Ingestion started - running every 5 minutes.");
    }

    @PostMapping("/stop")
    public ResponseEntity<String> stop() {
        jobManager.stopIngestion();
        return ResponseEntity.ok("Ingestion stopped.");
    }

    @GetMapping("/status")
    public ResponseEntity<String> status() {
        return ResponseEntity.ok("Current Status: " + jobManager.getStatus());
    }
}
