package com.hs.railway_stats.repository;

import com.hs.railway_stats.repository.entity.TripInfoMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.Optional;

@Repository
public interface TripInfoMetricRepository extends JpaRepository<TripInfoMetric, Integer> {

    Optional<TripInfoMetric> findByOriginIdAndDestinationIdAndScheduledDepartureTime(
            int originId, int destinationId, LocalTime scheduledDepartureTime);
}

