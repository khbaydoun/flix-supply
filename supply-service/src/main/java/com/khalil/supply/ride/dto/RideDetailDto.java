package com.khalil.supply.ride.dto;

import com.khalil.supply.ride.Ride;

import java.time.Instant;
import java.util.List;

public record RideDetailDto(RideDto ride, List<StopDto> stops) {

    public record StopDto(int sequenceNo, String stationName, Instant scheduledAt) {
    }

    public static RideDetailDto from(Ride r) {
        return new RideDetailDto(
                RideDto.from(r),
                r.getStops().stream()
                        .map(s -> new StopDto(s.getSequenceNo(), s.getStationName(), s.getScheduledAt()))
                        .toList());
    }
}
