package com.premierleague.live_match_tracker.kafka;
import com.example.pl_core_data.model.KafkaMatchEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class KafkaProducer {

    private final KafkaTemplate<String, KafkaMatchEvent> kafkaTemplate;

    public KafkaProducer(KafkaTemplate<String, KafkaMatchEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendEvent(KafkaMatchEvent kafkaMatchEvent) {
        String matchId = kafkaMatchEvent.getMatchId().toString();

        kafkaTemplate.send("match.events", matchId, kafkaMatchEvent)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Kafka event sent successfully! Match: {} | Offset: {}",
                                matchId, result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to send event for match: {}", matchId, ex);
                    }
                });
    }

}
