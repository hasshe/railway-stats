package com.hs.railway_stats.service;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.hs.railway_stats.dto.TripInfoResponse;
import com.hs.railway_stats.dto.TripResponse;
import com.hs.railway_stats.external.RestClient;
import com.hs.railway_stats.mapper.TripInfoMapper;

@Service
public class TripInfoServiceImpl implements TripInfoService {

    private RestClient restClient;
    public TripInfoServiceImpl(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<TripInfoResponse> getTripInfo(long originId, long destinationId) {
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
            return allTrips;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch trip info", e);
        }
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
