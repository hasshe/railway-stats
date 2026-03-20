package com.hs.railway_stats.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.hs.railway_stats.dto.TranslationDto;
import com.hs.railway_stats.dto.TripInfoResponse;
import com.hs.railway_stats.dto.TripResponse;
import com.hs.railway_stats.exception.TripCollectionException;
import com.hs.railway_stats.external.RestClient;
import com.hs.railway_stats.mapper.TripInfoMapper;
import com.hs.railway_stats.repository.TripInfoRepository;
import com.hs.railway_stats.repository.entity.TripInfo;
import com.hs.railway_stats.util.DateRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TripInfoServiceImpl implements TripInfoService {

    private static final Logger logger = LoggerFactory.getLogger(TripInfoServiceImpl.class);
    private static final int MINUTE = 59;
    private static final int HOUR = 23;
    public static final String ZONE_ID = "Europe/Stockholm";
    private static final ZoneId STOCKHOLM_ZONE = ZoneId.of(ZONE_ID);

    private final RestClient restClient;
    private final TripInfoRepository tripInfoRepository;
    private final TripInfoMetricService tripInfoMetricService;
    private final TranslationService translationService;
    private final Cache<String, List<TripInfoResponse>> tripInfoCache;

    public TripInfoServiceImpl(RestClient restClient,
                               TripInfoRepository tripInfoRepository,
                               TripInfoMetricService tripInfoMetricService,
                               TranslationService translationService,
                               @Qualifier("tripInfoCache") Cache<String, List<TripInfoResponse>> tripInfoCache) {
        this.restClient = restClient;
        this.tripInfoRepository = tripInfoRepository;
        this.tripInfoMetricService = tripInfoMetricService;
        this.translationService = translationService;
        this.tripInfoCache = tripInfoCache;
    }

    @Override
    @Transactional
    @Retryable(
            retryFor = TripCollectionException.class,
            maxAttemptsExpression = "${tripinfo.retry.max-attempts:3}",
            backoff = @Backoff(
                    delayExpression = "${tripinfo.retry.initial-interval-ms:5000}",
                    multiplierExpression = "${tripinfo.retry.multiplier:2.0}",
                    maxDelayExpression = "${tripinfo.retry.max-interval-ms:60000}"
            )
    )
    public void collectTripInformation(String originStationName, String destinationStationName) {
        try {
            TranslationDto origin = translationService.getTranslationByName(originStationName);
            TranslationDto destination = translationService.getTranslationByName(destinationStationName);
            long originId = origin.stationId();
            long destinationId = destination.stationId();
            LocalDate today = LocalDate.now(STOCKHOLM_ZONE);
            List<TripInfoResponse> allTrips = new ArrayList<>();
            var todayTrips = findAndFilterTrips(originId, destinationId, allTrips, today);
            var newTrips = saveTripInfoToDatabase(todayTrips, originId, destinationId);
            tripInfoMetricService.updateMetrics(newTrips, (int) originId, (int) destinationId, today);
        } catch (Exception e) {
            logger.error("Failed to collect trip information for {} to {}", originStationName, destinationStationName, e);
            throw new TripCollectionException(originStationName, destinationStationName, e);
        }
    }

    @Recover
    public void recoverCollectTripInformation(TripCollectionException e, String originStationName, String destinationStationName) {
        logger.error("All retry attempts exhausted for trip collection [{} -> {}]: {}",
                originStationName, destinationStationName, e.getMessage());
    }

    private List<TripInfoResponse> findAndFilterTrips(long originId, long destinationId, List<TripInfoResponse> allTrips,
                                                      LocalDate today) throws IOException, InterruptedException {
        boolean hasMoreData = true;
        String nextToken = null;
        while (hasMoreData) {
            TripResponse response = restClient.callSearch(originId, destinationId, nextToken);
            allTrips.addAll(TripInfoMapper.mapFromTripResponse(response));
            if (isLastTrainOfDay(response, today)) {
                hasMoreData = false;
            }
            nextToken = response != null ? response.nextToken() : null;
        }
        return allTrips.stream()
                .filter(trip -> trip.initialDepartureTime() != null
                        && trip.initialDepartureTime().atZoneSameInstant(STOCKHOLM_ZONE).toLocalDate().equals(today))
                .toList();
    }

    @Override
    public List<TripInfoResponse> getTripInfo(String originStationName, String destinationStationName, LocalDate date) {
        String cacheKey = originStationName + "-" + destinationStationName + "-" + date;
        List<TripInfoResponse> cached = tripInfoCache.getIfPresent(cacheKey);
        if (cached != null) {
            logger.info("CACHE HIT for key: {}", cacheKey);
            return cached;
        }
        logger.info("CACHE MISS (DB) for key: {}", cacheKey);
        TranslationDto origin = translationService.getTranslationByName(originStationName);
        TranslationDto destination = translationService.getTranslationByName(destinationStationName);
        DateRange range = DateRange.ofDay(date, STOCKHOLM_ZONE);

        List<TripInfo> tripInfos = tripInfoRepository.findByOriginAndDestinationAndDate(
                origin.stationId(), destination.stationId(), range.start(), range.end());

        List<TripInfoResponse> result = mapToResponses(tripInfos, origin, destination);
        if (!result.isEmpty()) {
            tripInfoCache.put(cacheKey, result);
        } else {
            logger.info("CACHE NOT STORED for key: {} (empty result)", cacheKey);
        }
        return result;
    }

    private List<TripInfoResponse> mapToResponses(List<TripInfo> tripInfos, TranslationDto origin, TranslationDto destination) {
        return tripInfos.stream()
                .map(info -> new TripInfoResponse(
                        origin.stationName(),
                        destination.stationName(),
                        info.getCanceled() == 1,
                        info.getMinutesLate(),
                        info.getOriginalDepartureTime() != null ? info.getOriginalDepartureTime().withZoneSameInstant(STOCKHOLM_ZONE).toOffsetDateTime() : null,
                        info.getActualArrivalTime() != null ? info.getActualArrivalTime().withZoneSameInstant(STOCKHOLM_ZONE).toOffsetDateTime() : null,
                        origin.claimsStationId(),
                        destination.claimsStationId()
                ))
                .toList();
    }

    private List<TripInfoResponse> saveTripInfoToDatabase(List<TripInfoResponse> trips, long originId, long destinationId) {
        List<TripInfoResponse> savedTrips = new ArrayList<>();
        for (TripInfoResponse trip : trips) {
            if (trip.initialDepartureTime() == null) {
                continue;
            }
            ZonedDateTime departureTime = trip.initialDepartureTime().atZoneSameInstant(STOCKHOLM_ZONE);
            boolean exists = tripInfoRepository.existsByOriginIdAndDestinationIdAndOriginalDepartureTime(
                    (int) originId, (int) destinationId, departureTime);
            if (exists) {
                logger.debug("Skipping duplicate trip: origin={} destination={} departure={}", originId, destinationId, departureTime);
                continue;
            }
            TripInfo tripInfo = TripInfo.builder()
                    .originId((int) originId)
                    .destinationId((int) destinationId)
                    .originalDepartureTime(departureTime)
                    .actualArrivalTime(trip.actualArrivalTime() != null ? trip.actualArrivalTime().atZoneSameInstant(STOCKHOLM_ZONE) : null)
                    .canceled(trip.isCancelled() ? 1 : 0)
                    .minutesLate(trip.totalMinutesLate())
                    .build();
            tripInfoRepository.save(tripInfo);
            savedTrips.add(trip);
        }
        return savedTrips;
    }

    @Override
    @Transactional
    public void deleteTripsByDate(LocalDate date) {
        DateRange range = DateRange.ofDay(date, STOCKHOLM_ZONE);
        tripInfoRepository.deleteByDate(range.start(), range.end());
        logger.info("Deleted all trip records for date {}", date);
    }

    @Override
    public void clearCache() {
        tripInfoCache.invalidateAll();
        logger.info("Trip info cache cleared by admin");
    }

    private boolean isLastTrainOfDay(final TripResponse response, final LocalDate today) {
        if (response == null || response.trips() == null || response.trips().isEmpty()) {
            return false;
        }
        var lastTrip = response.trips().getLast();
        if (lastTrip.legs() == null || lastTrip.legs().isEmpty()) {
            return false;
        }
        var lastLeg = lastTrip.legs().getLast();
        var plannedDeparture = lastLeg.origin().plannedDateTime();
        if (plannedDeparture == null) {
            return false;
        }
        var plannedDepartureStockholm = plannedDeparture.atZoneSameInstant(STOCKHOLM_ZONE);
        LocalDate departureDate = plannedDepartureStockholm.toLocalDate();
        if (!departureDate.equals(today)) {
            return true;
        }
        LocalTime endOfDay = LocalTime.of(HOUR, MINUTE);
        return !plannedDepartureStockholm.toLocalTime().isBefore(endOfDay);
    }
}
