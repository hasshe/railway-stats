package com.hs.railway_stats.service;

import com.hs.railway_stats.dto.TripInfoResponse;
import com.hs.railway_stats.repository.TranslationRepository;
import com.hs.railway_stats.repository.TripInfoMetricRepository;
import com.hs.railway_stats.repository.entity.Translation;
import com.hs.railway_stats.repository.entity.TripInfoMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public TripInfoMetricServiceImpl(TripInfoMetricRepository tripInfoMetricRepository,
                                     TranslationRepository translationRepository) {
        this.tripInfoMetricRepository = tripInfoMetricRepository;
        this.translationRepository = translationRepository;
    }

    @Override
    public List<TripInfoMetric> getMetrics(String originStationName, String destinationStationName) {
        int originId = stationNameToId(originStationName);
        int destinationId = stationNameToId(destinationStationName);
        return tripInfoMetricRepository.findByOriginIdAndDestinationId(originId, destinationId);
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

    private static void calculateMetrics(LocalDate today, TripInfoResponse trip, TripInfoMetric metric) {
        int n = metric.getTotalTrips();
        int newAvg = (metric.getAverageMinutesLate() * n + trip.totalMinutesLate()) / (n + 1);
        metric.setAverageMinutesLate(newAvg);
        metric.setTotalTrips(n + 1);

        if (trip.isCancelled()) {
            metric.getCanceledTripDates().add(today);
        }

        boolean reimbursable = trip.isCancelled() || trip.totalMinutesLate() >= REIMBURSABLE_MINUTES_THRESHOLD;
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
