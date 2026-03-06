package com.hs.railway_stats.exception;

public class StationNotFoundException extends RuntimeException {
    public StationNotFoundException(String stationName) {
        super("Station not found: " + stationName);
    }
}

