package com.hs.railway_stats.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StopTime(
        String name,
        OffsetDateTime plannedDateTime,
        OffsetDateTime actualDateTime
) {}