package com.hs.railway_stats.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StopTime(
        String name,
        OffsetDateTime plannedDateTime,
        OffsetDateTime actualDateTime
) {
}