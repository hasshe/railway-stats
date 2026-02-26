package com.hs.railway_stats.service;

import com.hs.railway_stats.repository.entity.Translation;

import java.util.List;

public interface TranslationService {

    Translation addStation(int stationId, String stationName);

    List<Translation> getAllStations();
}

