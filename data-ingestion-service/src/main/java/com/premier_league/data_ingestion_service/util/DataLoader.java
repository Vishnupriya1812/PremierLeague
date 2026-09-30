package com.premier_league.data_ingestion_service.util;

import com.premier_league.data_ingestion_service.service.MatchDetailService;
import com.premier_league.data_ingestion_service.service.MatchDiscoveryService;
import com.premier_league.data_ingestion_service.service.SeasonsDiscoveryService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {
    private final SeasonsDiscoveryService seasonsDiscoveryService;
    private final MatchDiscoveryService matchDiscoveryService;
    private final MatchDetailService matchDetailService;

    public DataLoader(SeasonsDiscoveryService seasonsDiscoveryService, MatchDiscoveryService matchDiscoveryService, MatchDetailService matchDetailService) {
        this.seasonsDiscoveryService = seasonsDiscoveryService;
        this.matchDiscoveryService = matchDiscoveryService;
        this.matchDetailService = matchDetailService;
    }

    public void run(String... args) {
        seasonsDiscoveryService.ingestAllSeasons();
//        matchDiscoveryService.ingestAllMatches();
        matchDetailService.ingestAllMatches();
    }
}
