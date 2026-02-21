package com.hs.railway_stats.external;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TripResponse(
        List<Trip> trips,
        String nextToken
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record Trip(
        Boolean canceled,
        List<Leg> legs
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record Leg(
        StopTime origin,
        StopTime destination
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record StopTime(
        String name,
        OffsetDateTime plannedDateTime,
        OffsetDateTime actualDateTime
) {}