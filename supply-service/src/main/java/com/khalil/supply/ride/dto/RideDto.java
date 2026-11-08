package com.khalil.supply.ride.dto;

import com.khalil.supply.brand.Brand;
import com.khalil.supply.ride.Ride;
import com.khalil.supply.ride.RideStatus;

import java.time.Instant;

public record RideDto(
        Long id,
        String lineCode,
        String origin,
        String destination,
        Long partnerId,
        String partnerName,
        String brandCode,
        Instant departureAt,
        Instant arrivalAt,
        int capacity,
        RideStatus status) {

    public static RideDto from(Ride r) {
        Brand brand = r.effectiveBrand();
        return new RideDto(
                r.getId(),
                r.getLine().getCode(),
                r.getLine().getOrigin(),
                r.getLine().getDestination(),
                r.getPartner().getId(),
                r.getPartner().getName(),
                brand == null ? null : brand.getCode(),
                r.getDepartureAt(),
                r.getArrivalAt(),
                r.getCapacity(),
                r.getStatus());
    }
}
