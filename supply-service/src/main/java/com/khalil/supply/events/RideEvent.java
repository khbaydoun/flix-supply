package com.khalil.supply.events;

import com.khalil.supply.brand.Brand;
import com.khalil.supply.ride.Ride;

import java.time.Instant;

/**
 * Integration event published to {@link KafkaTopics#RIDE_EVENTS}, keyed by ride id.
 * Each event carries the full current state of the ride, so the topic can be compacted
 * and consumed as a table.
 */
public record RideEvent(
        Type type,
        Long rideId,
        String lineCode,
        Long partnerId,
        String brandCode,
        Instant departureAt,
        Instant arrivalAt,
        int capacity,
        String status,
        Instant occurredAt) {

    public enum Type { RIDE_SCHEDULED, RIDE_CANCELLED, RIDE_SNAPSHOT }

    public static RideEvent of(Type type, Ride r) {
        Brand brand = r.effectiveBrand();
        return new RideEvent(type, r.getId(), r.getLine().getCode(), r.getPartner().getId(),
                brand == null ? null : brand.getCode(),
                r.getDepartureAt(), r.getArrivalAt(), r.getCapacity(), r.getStatus().name(), Instant.now());
    }
}
