package com.hs.railway_stats.dto;

public record TripInfoResponse(
    String startDestination,
    String endingDestination,
    Boolean isCancelled,
    int totalMinutesLate
) {}
