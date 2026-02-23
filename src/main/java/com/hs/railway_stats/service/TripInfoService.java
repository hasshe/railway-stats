package com.hs.railway_stats.service;

import java.time.LocalDate;
import java.util.List;

import com.hs.railway_stats.dto.TripInfoResponse;

public interface TripInfoService {

    List<TripInfoResponse> getTripInfo(long originId, long destinationId, LocalDate date);
    void collectTripInformation(long originId, long destinationId);
}
