package com.example.pl_core_data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KafkaMatchEvent {
    private String eventType; // "LINEUP", "INCIDENT", "STATUS"
    private Integer matchId;
    private Object payload;   // The actual data (Lineup, Incident, etc.)
}