package com.hs.railway_stats.dto;

import com.hs.railway_stats.repository.entity.Translation;

public record TranslationDto(
        int stationId,
        String stationName,
        String claimsStationId) {

    public static TranslationDto from(Translation t) {
        return new TranslationDto(t.getStationId(), t.getStationName(), t.getClaimsStationId());
    }
}

