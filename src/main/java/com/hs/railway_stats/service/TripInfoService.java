package com.hs.railway_stats.service;

import com.hs.railway_stats.dto.TripInfoResponse;

import java.time.LocalDate;
import java.util.List;

public interface TripInfoService {

    List<TripInfoResponse> getTripInfo(String originStationName, String destinationStationName, LocalDate date);

    void collectTripInformation(String originStationName, String destinationStationName);
}
