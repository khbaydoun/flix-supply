package com.khalil.supply.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Forwards {@link RideEvent}s to Kafka only after the DB transaction commits,
 * so consumers never see rides that were rolled back.
 */
@Component
public class RideEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RideEventPublisher.class);

    private final KafkaTemplate<String, RideEvent> kafka;

    public RideEventPublisher(KafkaTemplate<String, RideEvent> kafka) {
        this.kafka = kafka;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRideEvent(RideEvent event) {
        kafka.send(KafkaTopics.RIDE_EVENTS, String.valueOf(event.rideId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {} for ride {}", event.type(), event.rideId(), ex);
                    } else {
                        log.info("Published {} for ride {} to {}-{}@{}", event.type(), event.rideId(),
                                result.getRecordMetadata().topic(), result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
