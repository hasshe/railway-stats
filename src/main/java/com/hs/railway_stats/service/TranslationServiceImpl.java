package com.hs.railway_stats.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.hs.railway_stats.dto.TranslationDto;
import com.hs.railway_stats.exception.StationNotFoundException;
import com.hs.railway_stats.repository.TranslationRepository;
import com.hs.railway_stats.repository.entity.Translation;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.function.Supplier;

@Service
@Validated
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
    public Translation addStation(int stationId, @NotBlank String stationName, String claimsStationId) {
        Translation translation = Translation.builder()
                .stationId(stationId)
                .stationName(stationName.trim().toLowerCase())
                .claimsStationId(claimsStationId)
                .build();
        try {
            return translationRepository.save(translation);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException(
                    "Station with ID " + stationId + " or name '" + stationName + "' already exists.", e);
        }
    }

    @Override
    public int translateClaimsStationId(@NotBlank String claimsStationId) {
        return getOrLoad(
                cacheKeyForClaimsId(claimsStationId),
                () -> translationRepository.findByClaimsStationId(claimsStationId)
                        .map(TranslationDto::from)
                        .orElseThrow(() -> new StationNotFoundException(claimsStationId))
        ).stationId();
    }

    @Override
    public TranslationDto getTranslationByName(@NotBlank String stationName) {
        return getOrLoad(
                cacheKeyForName(stationName),
                () -> translationRepository.findByStationName(stationName.toLowerCase())
                        .map(TranslationDto::from)
                        .orElseThrow(() -> new StationNotFoundException(stationName))
        );
    }

    private TranslationDto getOrLoad(String key, Supplier<TranslationDto> loader) {
        TranslationDto cached = translationCache.getIfPresent(key);
        if (cached != null) {
            logger.debug("TRANSLATION CACHE HIT for key: {}", key);
            return cached;
        }
        logger.debug("TRANSLATION CACHE MISS (DB) for key: {}", key);
        TranslationDto dto = loader.get();
        cacheUnderAllKeys(dto);
        return dto;
    }

    private void cacheUnderAllKeys(TranslationDto dto) {
        translationCache.put(cacheKeyForName(dto.stationName()), dto);
        translationCache.put(cacheKeyForId(dto.stationId()), dto);
        if (dto.claimsStationId() != null) {
            translationCache.put(cacheKeyForClaimsId(dto.claimsStationId()), dto);
        }
    }

    private static String cacheKeyForName(String stationName) {
        return stationName.toLowerCase();
    }

    private static String cacheKeyForId(int stationId) {
        return String.valueOf(stationId);
    }

    private static String cacheKeyForClaimsId(String claimsStationId) {
        return claimsStationId;
    }

    @Override
    public void clearCache() {
        translationCache.invalidateAll();
        logger.info("Translation cache cleared by admin");
    }
}
