package com.premierleague.live_match_tracker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
public class LiveMatchesJobManager {

    private final TaskScheduler taskScheduler;
    private final LiveMatchData liveMatchData;
    private ScheduledFuture<?> scheduledTask;

    public void startIngestion(int round) {
        if (scheduledTask != null && !scheduledTask.isCancelled()) return;

        liveMatchData.initBrowser(); // Open browser once

        // Schedule to run every 5 minutes (300,000 ms)
        scheduledTask = taskScheduler.scheduleAtFixedRate(
                ()->liveMatchData.runScrapeCycle(round),
                Duration.ofMinutes(5)
        );
    }

    public void stopIngestion() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            liveMatchData.shutdownBrowser(); // Close browser on stop
        }
    }

    public String getStatus() {
        return (scheduledTask != null && !scheduledTask.isCancelled()) ? "RUNNING" : "STOPPED";
    }
}
