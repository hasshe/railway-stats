package com.hs.railway_stats.exception;

public class TripCollectionException extends RuntimeException {
    public TripCollectionException(String origin, String destination, Throwable cause) {
        super("Failed to collect trip information for route " + origin + " → " + destination, cause);
    }
}

