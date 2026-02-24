package com.hs.railway_stats.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TripResponse(
        List<Trip> trips,
        String nextToken
) {
}