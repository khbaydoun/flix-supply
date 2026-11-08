package com.khalil.supply.ride.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

public record CreateRideRequest(
        @NotBlank String lineCode,
        @NotNull Long partnerId,
        /* Optional: when omitted, the ride inherits the line's brand. */
        String brandCode,
        @NotNull @Future Instant departureAt,
        @NotNull @Future Instant arrivalAt,
        @Positive int capacity,
        List<@Valid StopRequest> stops) {

    public record StopRequest(@NotBlank String stationName, @NotNull Instant scheduledAt) {
    }

    @AssertTrue(message = "arrivalAt must be after departureAt")
    public boolean isArrivalAfterDeparture() {
        return departureAt == null || arrivalAt == null || arrivalAt.isAfter(departureAt);
    }
}
