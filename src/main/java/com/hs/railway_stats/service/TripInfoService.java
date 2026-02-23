package com.hs.railway_stats.service;

import java.time.LocalDate;
import java.util.List;

import com.hs.railway_stats.dto.TripInfoResponse;

public interface TripInfoService {

    List<TripInfoResponse> getTripInfo(String originStationName, String destinationStationName, LocalDate date);
    void collectTripInformation(String originStationName, String destinationStationName);
}
