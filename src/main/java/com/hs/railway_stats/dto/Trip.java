package com.hs.railway_stats.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Trip(
        Boolean canceled,
        List<Leg> legs
) {
}
