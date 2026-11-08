package com.khalil.supply.web;

/** Base class for "resource does not exist" errors; mapped to HTTP 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
