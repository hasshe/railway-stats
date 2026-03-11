package com.hs.railway_stats.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.hs.railway_stats.dto.TranslationDto;
import com.hs.railway_stats.exception.StationNotFoundException;
import com.hs.railway_stats.repository.TranslationRepository;
import com.hs.railway_stats.repository.entity.Translation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class TranslationServiceImpl implements TranslationService {

    private static final Logger logger = LoggerFactory.getLogger(TranslationServiceImpl.class);

    private final TranslationRepository translationRepository;
    private final Cache<String, TranslationDto> translationCache;

    public TranslationServiceImpl(TranslationRepository translationRepository,
                                  @Qualifier("translationCache") Cache<String, TranslationDto> translationCache) {
        this.translationRepository = translationRepository;
        this.translationCache = translationCache;
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
    public int translateClaimsStationId(String claimsStationId) {
        TranslationDto cached = translationCache.getIfPresent(claimsStationId);
        if (cached != null) {
            logger.info("TRANSLATION CACHE HIT for claimsStationId: {}", claimsStationId);
            return cached.stationId();
        }
        logger.info("TRANSLATION CACHE MISS (DB) for claimsStationId: {}", claimsStationId);
        Translation translation = translationRepository.findByClaimsStationId(claimsStationId)
                .orElseThrow(() -> new IllegalArgumentException("No station found for claims station ID: " + claimsStationId));
        TranslationDto dto = new TranslationDto(translation.getStationId(), translation.getStationName(), translation.getClaimsStationId());
        cacheUnderAllKeys(dto);
        return dto.stationId();
    }

    @Override
    public TranslationDto getTranslationByName(String stationName) {
        String key = stationName.toLowerCase();
        TranslationDto cached = translationCache.getIfPresent(key);
        if (cached != null) {
            logger.info("TRANSLATION CACHE HIT for station: {}", stationName);
            return cached;
        }
        logger.info("TRANSLATION CACHE MISS (DB) for station: {}", stationName);
        Translation t = translationRepository.findByStationName(key)
                .orElseThrow(() -> new StationNotFoundException(stationName));
        TranslationDto dto = new TranslationDto(t.getStationId(), t.getStationName(), t.getClaimsStationId());
        cacheUnderAllKeys(dto);
        return dto;
    }

    private void cacheUnderAllKeys(TranslationDto dto) {
        translationCache.put(dto.stationName().toLowerCase(), dto);
        if (dto.claimsStationId() != null) {
            translationCache.put(dto.claimsStationId(), dto);
        }
        translationCache.put(String.valueOf(dto.stationId()), dto);
    }

    @Override
    public void clearCache() {
        translationCache.invalidateAll();
        logger.info("Translation cache cleared by admin");
    }
}
