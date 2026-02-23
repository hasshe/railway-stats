package com.hs.railway_stats.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.hs.railway_stats.dto.TripInfoResponse;
import com.hs.railway_stats.dto.TripResponse;
import com.hs.railway_stats.external.RestClient;
import com.hs.railway_stats.mapper.TripInfoMapper;
import com.hs.railway_stats.repository.TranslationRepository;
import com.hs.railway_stats.repository.TripInfoRepository;
import com.hs.railway_stats.repository.entity.Translation;
import com.hs.railway_stats.repository.entity.TripInfo;

import jakarta.transaction.Transactional;

@Service
public class TripInfoServiceImpl implements TripInfoService {

    private static final int MINUTE = 59;
    private static final int HOUR = 23;
    private static final int UPPSALA_ID = 740000005;
    private static final int STOCKHOLM_ID = 740000001;
    private static final String UPPSALA_NAME = "Uppsala";
    private static final String STOCKHOLM_NAME = "Stockholm";
    private RestClient restClient;
    private TripInfoRepository tripInfoRepository;
    private TranslationRepository translationRepository;

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
            
            String nextToken = null;
            List<TripInfoResponse> allTrips = new java.util.ArrayList<>();
            LocalDate today = LocalDate.now();
            boolean hasMoreData = true;
            while(hasMoreData) {
                TripResponse response = restClient.callSearch(originId, destinationId, nextToken);
                var mappedTrips = TripInfoMapper.mapFromTripResponse(response);
                allTrips.addAll(mappedTrips);

                if (isLastTrainOfDay(response, today)) {
                    hasMoreData = false;
                }
                nextToken = response != null ? response.nextToken() : null;
            }
            List<TripInfoResponse> todayTrips = allTrips.stream()
                .filter(trip -> trip.initialDepartureTime() != null 
                    && trip.initialDepartureTime().toLocalDate().equals(today))
                .toList();
            saveTripInfoToDatabase(todayTrips, originId, destinationId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch trip info", e);
        }
    }

    @Override
    public List<TripInfoResponse> getTripInfo(String originStationName, String destinationStationName, LocalDate date) {
        long originId = stationNameToDestinationId(originStationName);
        long destinationId = stationNameToDestinationId(destinationStationName);
        
        List<TripInfo> tripInfos = tripInfoRepository.findAll();
        return tripInfos.stream()
            .filter(info -> info.getOriginId() == originId && info.getDestinationId() == destinationId)
            .filter(info -> info.getOriginalDepartureTime() != null && info.getOriginalDepartureTime().toLocalDate().equals(date))
            .map(info -> new TripInfoResponse(
                destinationIdToStationName(info.getOriginId()),
                destinationIdToStationName(info.getDestinationId()),
                info.getCanceled() == 1,
                info.getMinutesLate(),
                info.getOriginalDepartureTime() != null ? info.getOriginalDepartureTime().toOffsetDateTime() : null,
                info.getActualArrivalTime() != null ? info.getActualArrivalTime().toOffsetDateTime() : null
            ))
            .toList();
    }

    private void saveTripInfoToDatabase(List<TripInfoResponse> trips, long originId, long destinationId) {
        trips.forEach(trip -> {
            TripInfo tripInfo = TripInfo.builder()
                .originId((int) originId)
                .destinationId((int) destinationId)
                .originalDepartureTime(trip.initialDepartureTime() != null ? trip.initialDepartureTime().toZonedDateTime() : null)
                .actualArrivalTime(trip.actualArrivalTime() != null ? trip.actualArrivalTime().toZonedDateTime() : null)
                .canceled(trip.isCancelled() ? 1 : 0)
                .minutesLate(trip.totalMinutesLate())
                .build();
            tripInfoRepository.save(tripInfo);
        });
    }

    @Scheduled(cron = "59 40 23 * * ?")
    protected final void scheduleRun() {
        getTripInfo(UPPSALA_NAME, STOCKHOLM_NAME, LocalDate.now());
        getTripInfo(STOCKHOLM_NAME, UPPSALA_NAME, LocalDate.now());
    }

    private long stationNameToDestinationId(String stationName) {
        Translation translation = translationRepository.findByStationName(stationName)
            .orElseThrow(() -> new RuntimeException("Station not found: " + stationName));
        return translation.getDestinationId();
    }

    private String destinationIdToStationName(int destinationId) {
        Translation translation = translationRepository.findByDestinationId(destinationId)
            .orElseThrow(() -> new RuntimeException("Destination ID not found: " + destinationId));
        return translation.getStationName();
    }

    private boolean isLastTrainOfDay(final TripResponse response, final LocalDate today) {
        if (response == null || response.trips() == null
        || response.trips().isEmpty()) {
            return false;
        }
        var lastTrip = response.trips().get(response.trips().size() - 1);
        if (lastTrip.legs() == null || lastTrip.legs().isEmpty()) {
            return false;
        }
        var lastLeg = lastTrip.legs().get(lastTrip.legs().size() - 1);
        var plannedDeparture = lastLeg.origin().plannedDateTime();
        if (plannedDeparture == null) {
            return false;
        }
        LocalDate departureDate = plannedDeparture.toLocalDate();
        if (!departureDate.equals(today)) {
            return true;
        }
        var endOfDay = plannedDeparture.toLocalDate().atTime(HOUR, MINUTE);
        return !plannedDeparture.toLocalDateTime().isBefore(endOfDay);
    }
}