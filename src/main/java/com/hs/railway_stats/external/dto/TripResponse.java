package com.hs.railway_stats.external.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TripResponse(
        List<Trip> trips,
        String nextToken
) {}