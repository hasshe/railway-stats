package com.hs.railway_stats.service;

import com.hs.railway_stats.dto.TripInfoResponse;
import com.hs.railway_stats.repository.entity.TripInfoMetric;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface TripInfoMetricService {

    List<TripInfoMetric> getMetrics(String originStationName, String destinationStationName);

    List<LocalTime> getDepartureTimes(String originStationName, String destinationStationName);

    void updateMetrics(List<TripInfoResponse> trips, int originId, int destinationId, LocalDate today);
}
