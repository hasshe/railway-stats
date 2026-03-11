package com.hs.railway_stats.service;

import com.hs.railway_stats.dto.TranslationDto;
import com.hs.railway_stats.exception.StationNotFoundException;
import com.hs.railway_stats.repository.TranslationRepository;
import com.hs.railway_stats.repository.entity.Translation;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TranslationServiceImpl implements TranslationService {

    private final TranslationRepository translationRepository;

    public TranslationServiceImpl(TranslationRepository translationRepository) {
        this.translationRepository = translationRepository;
    }

    @Override
    public Translation addStation(int stationId, String stationName) {
        return addStation(stationId, stationName, null);
    }

    @Override
    public Translation addStation(int stationId, String stationName, String claimsStationId) {
        translationRepository.findByStationId(stationId).ifPresent(t -> {
            throw new IllegalArgumentException("Station with ID " + stationId + " already exists: " + t.getStationName());
        });
        translationRepository.findByStationName(stationName).ifPresent(t -> {
            throw new IllegalArgumentException("Station with name '" + stationName + "' already exists.");
        });
        Translation translation = Translation.builder()
                .stationId(stationId)
                .stationName(stationName)
                .claimsStationId(claimsStationId)
                .build();
        return translationRepository.save(translation);
    }

    @Override
    public List<Translation> getAllStations() {
        return translationRepository.findAll();
    }

    @Override
    public int translateClaimsStationId(String claimsStationId) {
        return translationRepository.findByClaimsStationId(claimsStationId)
                .orElseThrow(() -> new IllegalArgumentException("No station found for claims station ID: " + claimsStationId))
                .getStationId();
    }

    @Override
    public String getClaimsStationId(int stationId) {
        return translationRepository.findByStationId(stationId)
                .map(Translation::getClaimsStationId)
                .orElse(null);
    }

    @Override
    public int getStationIdByName(String stationName) {
        return translationRepository.findByStationName(stationName.toLowerCase())
                .orElseThrow(() -> new StationNotFoundException(stationName))
                .getStationId();
    }

    @Override
    public String getStationNameById(int stationId) {
        return translationRepository.findByStationId(stationId)
                .orElseThrow(() -> new StationNotFoundException(String.valueOf(stationId)))
                .getStationName();
    }

    @Override
    public TranslationDto getTranslationByName(String stationName) {
        Translation t = translationRepository.findByStationName(stationName.toLowerCase())
                .orElseThrow(() -> new StationNotFoundException(stationName));
        return new TranslationDto(t.getStationId(), t.getStationName(), t.getClaimsStationId());
    }
}
