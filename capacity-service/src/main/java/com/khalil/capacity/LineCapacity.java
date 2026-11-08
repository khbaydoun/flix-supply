package com.khalil.capacity;

/** Aggregated seat supply of all currently scheduled rides on a line. */
public record LineCapacity(String lineCode, long scheduledRides, long totalSeats) {

    public static LineCapacity empty() {
        return new LineCapacity(null, 0, 0);
    }

    public LineCapacity add(String line, RideEvent ride) {
        return ride.isScheduled()
                ? new LineCapacity(line, scheduledRides + 1, totalSeats + ride.capacity())
                : new LineCapacity(line, scheduledRides, totalSeats);
    }

    public LineCapacity subtract(String line, RideEvent ride) {
        return ride.isScheduled()
                ? new LineCapacity(line, scheduledRides - 1, totalSeats - ride.capacity())
                : new LineCapacity(line, scheduledRides, totalSeats);
    }
}
