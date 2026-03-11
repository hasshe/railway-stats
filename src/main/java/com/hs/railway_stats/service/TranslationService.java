package com.hs.railway_stats.service;

import com.hs.railway_stats.dto.TranslationDto;
import com.hs.railway_stats.repository.entity.Translation;

import java.util.List;

public interface TranslationService {

    Translation addStation(int stationId, String stationName);

    Translation addStation(int stationId, String stationName, String claimsStationId);

    List<Translation> getAllStations();

    int translateClaimsStationId(String claimsStationId);

    String getClaimsStationId(int stationId);

    int getStationIdByName(String stationName);

    String getStationNameById(int stationId);

    TranslationDto getTranslationByName(String stationName);
}
