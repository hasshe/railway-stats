package com.hs.railway_stats.mapper;

import java.time.Duration;
import java.util.List;

import com.hs.railway_stats.dto.TripInfoResponse;
import com.hs.railway_stats.dto.TripResponse;

public class TripInfoMapper {

    private TripInfoMapper() {
        // Private constructor to prevent instantiation
    }
    public static List<TripInfoResponse> mapFromTripResponse(TripResponse tripResponse) {
        if (tripResponse == null || tripResponse.trips() == null) return List.of();
        return tripResponse.trips().stream()
            .filter(trip -> trip.legs() != null && !trip.legs().isEmpty())
            .map(trip -> {
                var firstLeg = trip.legs().get(0);
                var lastLeg = trip.legs().get(trip.legs().size() - 1);
                String startDestination = firstLeg.origin().name();
                String endingDestination = lastLeg.destination().name();
                Boolean isCancelled = trip.canceled();
                var planned = lastLeg.destination().plannedDateTime();
                var actual = lastLeg.destination().actualDateTime();
                int totalMinutesLate = 0;
                if (planned != null && actual != null) {
                    totalMinutesLate = (int) Duration.between(planned, actual).toMinutes();
                }
                return new TripInfoResponse(startDestination, endingDestination, isCancelled, totalMinutesLate);
            })
            .toList();
    }
}
