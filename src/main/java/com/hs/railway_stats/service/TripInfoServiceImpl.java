package com.hs.railway_stats.service;

import com.hs.railway_stats.config.StationConstants;
import com.hs.railway_stats.dto.TripInfoResponse;
import com.hs.railway_stats.dto.TripResponse;
import com.hs.railway_stats.external.RestClient;
import com.hs.railway_stats.mapper.TripInfoMapper;
import com.hs.railway_stats.repository.TranslationRepository;
import com.hs.railway_stats.repository.TripInfoRepository;
import com.hs.railway_stats.repository.entity.Translation;
import com.hs.railway_stats.repository.entity.TripInfo;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
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
    private final RestClient restClient;
    private final TripInfoRepository tripInfoRepository;
    private final TranslationRepository translationRepository;

    public TripInfoServiceImpl(RestClient restClient, TripInfoRepository tripInfoRepository, TranslationRepository translationRepository) {
        this.restClient = restClient;
        this.tripInfoRepository = tripInfoRepository;
        this.translationRepository = translationRepository;
    }

    @Override
    @Transactional
    public void collectTripInformation(String originStationName, String destinationStationName) {
        try {
            long originId = stationNameToDestinationId(originStationName);
            long destinationId = stationNameToDestinationId(destinationStationName);
            List<TripInfoResponse> allTrips = new ArrayList<>();
            ZoneId stockholmZone = ZoneId.of(ZONE_ID);
            LocalDate today = LocalDate.now(stockholmZone);
            var todayTrips = findAndFilterTrips(originId, destinationId, allTrips, today);
            saveTripInfoToDatabase(todayTrips, originId, destinationId);
        } catch (Exception e) {
            logger.error("Failed to collect trip information for {} to {}", originStationName, destinationStationName, e);
            throw new RuntimeException("Failed to fetch trip info", e);
        }
    }

    private List<TripInfoResponse> findAndFilterTrips(long originId, long destinationId, List<TripInfoResponse> allTrips,
                                                      LocalDate today) throws IOException, InterruptedException {
        ZoneId stockholmZone = ZoneId.of(ZONE_ID);
        boolean hasMoreData = true;
        String nextToken = null;
        while (hasMoreData) {
            TripResponse response = restClient.callSearch(originId, destinationId, nextToken);
            var mappedTrips = TripInfoMapper.mapFromTripResponse(response);
            allTrips.addAll(mappedTrips);

            if (isLastTrainOfDay(response, today)) {
                hasMoreData = false;
            }
            nextToken = response != null ? response.nextToken() : null;
        }
        return allTrips.stream()
                .filter(trip -> trip.initialDepartureTime() != null
                        && trip.initialDepartureTime().atZoneSameInstant(stockholmZone).toLocalDate().equals(today))
                .toList();
    }

    @Override
    public List<TripInfoResponse> getTripInfo(String originStationName, String destinationStationName, LocalDate date) {
        long originId = stationNameToDestinationId(originStationName);
        long destinationId = stationNameToDestinationId(destinationStationName);
        ZoneId stockholmZone = ZoneId.of(ZONE_ID);

        ZonedDateTime startOfDay = date.atStartOfDay(stockholmZone);
        ZonedDateTime endOfDay = date.plusDays(1).atStartOfDay(stockholmZone);

        String originClaimsStationId = translationRepository.findByStationId((int) originId)
                .map(Translation::getClaimsStationId)
                .orElse(null);
        String destinationClaimsStationId = translationRepository.findByStationId((int) destinationId)
                .map(Translation::getClaimsStationId)
                .orElse(null);

        List<TripInfo> tripInfos = tripInfoRepository.findByOriginAndDestinationAndDate(
                (int) originId, (int) destinationId, startOfDay, endOfDay);

        return tripInfos.stream()
                .map(info -> new TripInfoResponse(
                        destinationIdToStationName(info.getOriginId()),
                        destinationIdToStationName(info.getDestinationId()),
                        info.getCanceled() == 1,
                        info.getMinutesLate(),
                        info.getOriginalDepartureTime() != null ? info.getOriginalDepartureTime().withZoneSameInstant(stockholmZone).toOffsetDateTime() : null,
                        info.getActualArrivalTime() != null ? info.getActualArrivalTime().withZoneSameInstant(stockholmZone).toOffsetDateTime() : null,
                        originClaimsStationId,
                        destinationClaimsStationId
                ))
                .toList();
    }

    private void saveTripInfoToDatabase(List<TripInfoResponse> trips, long originId, long destinationId) {
        ZoneId stockholmZone = ZoneId.of(ZONE_ID);
        trips.forEach(trip -> {
            TripInfo tripInfo = TripInfo.builder()
                    .originId((int) originId)
                    .destinationId((int) destinationId)
                    .originalDepartureTime(trip.initialDepartureTime() != null ? trip.initialDepartureTime().atZoneSameInstant(stockholmZone) : null)
                    .actualArrivalTime(trip.actualArrivalTime() != null ? trip.actualArrivalTime().atZoneSameInstant(stockholmZone) : null)
                    .canceled(trip.isCancelled() ? 1 : 0)
                    .minutesLate(trip.totalMinutesLate())
                    .build();
            tripInfoRepository.save(tripInfo);
        });
    }

    @Override
    @Transactional
    public void deleteTripsByDate(LocalDate date) {
        ZoneId stockholmZone = ZoneId.of(ZONE_ID);
        ZonedDateTime startOfDay = date.atStartOfDay(stockholmZone);
        ZonedDateTime endOfDay = date.plusDays(1).atStartOfDay(stockholmZone);
        tripInfoRepository.deleteByDate(startOfDay, endOfDay);
        logger.info("Deleted all trip records for date {}", date);
    }

    @Scheduled(cron = "59 50 23 * * ?", zone = ZONE_ID)
    protected final void scheduleRun() {
        logger.info("Starting scheduled trip information collection job");
        try {
            collectTripInformation(StationConstants.UPPSALA, StationConstants.STOCKHOLM);
            collectTripInformation(StationConstants.STOCKHOLM, StationConstants.UPPSALA);
            logger.info("Scheduled trip information collection job completed successfully");
        } catch (Exception e) {
            logger.error("Scheduled trip information collection job failed", e);
            throw e;
        }
    }

    private long stationNameToDestinationId(String stationName) {
        Translation translation = translationRepository.findByStationName(stationName.toLowerCase())
                .orElseThrow(() -> new RuntimeException("Station not found: " + stationName));
        return translation.getStationId();
    }

    private String destinationIdToStationName(int destinationId) {
        Translation translation = translationRepository.findByStationId(destinationId)
                .orElseThrow(() -> new RuntimeException("Destination ID not found: " + destinationId));
        return translation.getStationName();
    }

    private boolean isLastTrainOfDay(final TripResponse response, final LocalDate today) {
        if (response == null || response.trips() == null
                || response.trips().isEmpty()) {
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
        ZoneId stockholmZone = ZoneId.of(ZONE_ID);
        var plannedDepartureStockholm = plannedDeparture.atZoneSameInstant(stockholmZone);
        LocalDate departureDate = plannedDepartureStockholm.toLocalDate();
        if (!departureDate.equals(today)) {
            return true;
        }
        var endOfDay = departureDate.atTime(HOUR, MINUTE);
        return !plannedDepartureStockholm.toLocalDateTime().isBefore(endOfDay);
    }
}