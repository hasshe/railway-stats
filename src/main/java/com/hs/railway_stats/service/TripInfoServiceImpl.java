package com.hs.railway_stats.service;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.hs.railway_stats.dto.TripInfoResponse;
import com.hs.railway_stats.dto.TripResponse;
import com.hs.railway_stats.external.RestClient;
import com.hs.railway_stats.mapper.TripInfoMapper;
import com.hs.railway_stats.repository.TripInfoRepository;
import com.hs.railway_stats.repository.entity.TripInfo;

@Service
public class TripInfoServiceImpl implements TripInfoService {

    private RestClient restClient;
    private TripInfoRepository tripInfoRepository;

    public TripInfoServiceImpl(RestClient restClient, TripInfoRepository tripInfoRepository) {
        this.restClient = restClient;
        this.tripInfoRepository = tripInfoRepository;
    }

    @Override
    public void collectTripInformation(long originId, long destinationId) {
        try {
            String nextToken = null;
            List<TripInfoResponse> allTrips = new java.util.ArrayList<>();
            for (int i = 0; i < 7; i++) {
                TripResponse response = restClient.callSearch(originId, destinationId, nextToken);
                var mappedTrips = TripInfoMapper.mapFromTripResponse(response);
                allTrips.addAll(mappedTrips);

                if (isLastTrainOfDay(response)) {
                    break;
                }
                nextToken = response != null ? response.nextToken() : null;
            }
            saveTripInfoToDatabase(allTrips, originId, destinationId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch trip info", e);
        }
    }

    @Override
    public List<TripInfoResponse> getTripInfo(long originId, long destinationId) {
        List<TripInfo> tripInfos = tripInfoRepository.findAll();
        return tripInfos.stream()
            .filter(info -> info.getOriginId() == originId && info.getDestinationId() == destinationId)
            .map(info -> new TripInfoResponse(
                String.valueOf(info.getOriginId()),
                String.valueOf(info.getDestinationId()),
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
    private void scheduleRun() {
        getTripInfo(740000005, 740000001);
        getTripInfo(740000001, 740000005);
    }

    private boolean isLastTrainOfDay(TripResponse response) {
        if (response == null || response.trips() == null || response.trips().isEmpty()) {
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
        var endOfDay = plannedDeparture.toLocalDate().atTime(23, 59);
        return !plannedDeparture.toLocalDateTime().isBefore(endOfDay);
    }
}
