package com.hs.railway_stats.dto;

import java.time.OffsetDateTime;

public record TripInfoResponse(
    String startDestination,
    String endingDestination,
    Boolean isCancelled,
    int totalMinutesLate,
    OffsetDateTime initialDepartureTime,
    OffsetDateTime actualArrivalTime
) { }
