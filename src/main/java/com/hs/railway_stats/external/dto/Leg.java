package com.hs.railway_stats.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Leg(
        StopTime origin,
        StopTime destination
) {}