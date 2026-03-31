package com.hs.railway_stats.view.component;

import lombok.Getter;

@Getter
public enum TripFilter {
    CLAIMABLE_ALL("Claimable (All)"),
    LATE_20("Late (≥ 20min)"),
    LATE_ALL("Late (All)"),
    CANCELLED("Cancelled"),
    NO_FILTER("No Filter");

    private final String label;

    TripFilter(String label) { this.label = label; }

}

