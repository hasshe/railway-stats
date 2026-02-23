package com.hs.railway_stats.service;

import java.util.List;

import com.hs.railway_stats.dto.TripInfoResponse;

public interface TripInfoService {

    List<TripInfoResponse> getTripInfo(long originId, long destinationId);
    void collectTripInformation(long originId, long destinationId);
}
