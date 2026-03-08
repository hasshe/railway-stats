package com.hs.railway_stats.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.hs.railway_stats.config.StationConstants;
import com.hs.railway_stats.dto.TripInfoResponse;
import com.hs.railway_stats.repository.TranslationRepository;
import com.hs.railway_stats.repository.TripInfoMetricRepository;
import com.hs.railway_stats.repository.entity.Translation;
import com.hs.railway_stats.repository.entity.TripInfoMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class TripInfoMetricServiceImpl implements TripInfoMetricService {

    private static final Logger logger = LoggerFactory.getLogger(TripInfoMetricServiceImpl.class);
    private static final int REIMBURSABLE_MINUTES_THRESHOLD = 20;
    private static final String ZONE_ID = "Europe/Stockholm";

    private final TripInfoMetricRepository tripInfoMetricRepository;
    private final TranslationRepository translationRepository;
    private final Cache<String, List<TripInfoMetric>> metricsCache;

    public TripInfoMetricServiceImpl(TripInfoMetricRepository tripInfoMetricRepository,
                                     TranslationRepository translationRepository,
                                     @Qualifier("metricsCache") Cache<String, List<TripInfoMetric>> metricsCache) {
        this.tripInfoMetricRepository = tripInfoMetricRepository;
        this.translationRepository = translationRepository;
        this.metricsCache = metricsCache;
    }

    @Override
    public List<TripInfoMetric> getMetrics(String originStationName, String destinationStationName) {
        String cacheKey = originStationName + "-" + destinationStationName;
        List<TripInfoMetric> cached = metricsCache.getIfPresent(cacheKey);
        if (cached != null) {
            logger.info("METRICS CACHE HIT for key: {}", cacheKey);
            return cached;
        }
        logger.info("METRICS CACHE MISS (DB) for key: {}", cacheKey);
        int originId = stationNameToId(originStationName);
        int destinationId = stationNameToId(destinationStationName);
        List<TripInfoMetric> result = tripInfoMetricRepository.findByOriginIdAndDestinationId(originId, destinationId);
        if (!result.isEmpty()) {
            metricsCache.put(cacheKey, result);
        }
        return result;
    }

    @Override
    public List<LocalTime> getDepartureTimes(String originStationName, String destinationStationName) {
        return getMetrics(originStationName, destinationStationName).stream()
                .map(TripInfoMetric::getScheduledDepartureTime)
                .sorted()
                .toList();
    }

    @Override
    public void updateMetrics(List<TripInfoResponse> trips, int originId, int destinationId, LocalDate today) {
        ZoneId stockholmZone = ZoneId.of(ZONE_ID);
        trips.forEach(trip -> {
            if (trip.initialDepartureTime() == null) {
                return;
            }
            if (trip.actualArrivalTime() == null && !trip.isCancelled()) {
                return;
            }

            LocalTime scheduledDeparture = getScheduledDeparture(trip, stockholmZone);

            TripInfoMetric metric = getTripInfoMetric(originId, destinationId, scheduledDeparture);

            calculateMetrics(today, trip, metric);

            tripInfoMetricRepository.save(metric);
            logger.debug("Updated metric for origin={} destination={} departure={}", originId, destinationId, scheduledDeparture);
        });
    }

    @Scheduled(cron = "0 59 23 * * ?", zone = ZONE_ID)
    protected final void refreshMetricsCache() {
        logger.info("Refreshing metrics cache for all station pairs");
        metricsCache.invalidateAll();
        logger.info("Metrics cache cleared");
        for (String origin : StationConstants.ALL_STATIONS) {
            for (String destination : StationConstants.ALL_STATIONS) {
                if (!origin.equals(destination)) {
                    try {
                        String cacheKey = origin + "-" + destination;
                        int originId = stationNameToId(origin);
                        int destinationId = stationNameToId(destination);
                        List<TripInfoMetric> metrics = tripInfoMetricRepository.findByOriginIdAndDestinationId(originId, destinationId);
                        if (!metrics.isEmpty()) {
                            metricsCache.put(cacheKey, metrics);
                            logger.info("Metrics cache repopulated for key: {}", cacheKey);
                        }
                    } catch (Exception e) {
                        logger.error("Failed to repopulate metrics cache for {} -> {}", origin, destination, e);
                    }
                }
            }
        }
        logger.info("Metrics cache refresh complete");
    }

    @Override
    public void clearCache() {
        metricsCache.invalidateAll();
        logger.info("Metrics cache cleared by admin");
    }

    private static void calculateMetrics(LocalDate today, TripInfoResponse trip, TripInfoMetric metric) {
        int n = metric.getTotalTrips();
        int minutesLate = Math.max(0, trip.totalMinutesLate());
        int newAvg = (metric.getAverageMinutesLate() * n + minutesLate) / (n + 1);
        metric.setAverageMinutesLate(newAvg);
        metric.setTotalTrips(n + 1);

        if (trip.isCancelled()) {
            metric.getCanceledTripDates().add(today);
        }

        boolean reimbursable = trip.isCancelled() || minutesLate >= REIMBURSABLE_MINUTES_THRESHOLD;
        if (reimbursable) {
            metric.setTotalReimbursableTrips(metric.getTotalReimbursableTrips() + 1);
        }
    }

    private static LocalTime getScheduledDeparture(TripInfoResponse trip, ZoneId stockholmZone) {
        return trip.initialDepartureTime()
                .atZoneSameInstant(stockholmZone)
                .toLocalTime();
    }

    private TripInfoMetric getTripInfoMetric(int originId, int destinationId, LocalTime scheduledDeparture) {
        return tripInfoMetricRepository
                .findByOriginIdAndDestinationIdAndScheduledDepartureTime(originId, destinationId, scheduledDeparture)
                .orElseGet(() -> TripInfoMetric.builder()
                        .originId(originId)
                        .destinationId(destinationId)
                        .scheduledDepartureTime(scheduledDeparture)
                        .build());
    }

    private int stationNameToId(String stationName) {
        Translation translation = translationRepository.findByStationName(stationName.toLowerCase())
                .orElseThrow(() -> new RuntimeException("Station not found: " + stationName));
        return translation.getStationId();
    }
}
