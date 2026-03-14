package com.hs.railway_stats.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public record DateRange(ZonedDateTime start, ZonedDateTime end) {

    public static DateRange ofDay(LocalDate date, ZoneId zone) {
        return new DateRange(
                date.atStartOfDay(zone),
                date.plusDays(1).atStartOfDay(zone)
        );
    }
}

