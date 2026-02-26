package com.hs.railway_stats.service;

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
        translationRepository.findByStationId(stationId).ifPresent(t -> {
            throw new IllegalArgumentException("Station with ID " + stationId + " already exists: " + t.getStationName());
        });
        translationRepository.findByStationName(stationName).ifPresent(t -> {
            throw new IllegalArgumentException("Station with name '" + stationName + "' already exists.");
        });
        Translation translation = Translation.builder()
                .stationId(stationId)
                .stationName(stationName)
                .build();
        return translationRepository.save(translation);
    }

    @Override
    public List<Translation> getAllStations() {
        return translationRepository.findAll();
    }
}

