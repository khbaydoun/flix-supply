package com.khalil.capacity;

import java.time.Instant;

/** Consumer-side view of the supply-service ride event (JSON contract, no shared jar). */
public record RideEvent(
        String type,
        Long rideId,
        String lineCode,
        Long partnerId,
        String brandCode,
        Instant departureAt,
        Instant arrivalAt,
        int capacity,
        String status,
        Instant occurredAt) {

    public boolean isScheduled() {
        return "SCHEDULED".equals(status);
    }
}
