package com.khalil.supply.ride;

import com.khalil.supply.web.NotFoundException;

public class RideNotFoundException extends NotFoundException {
    public RideNotFoundException(Long id) {
        super("Ride not found: " + id);
    }
}
