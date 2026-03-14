package com.hs.railway_stats.repository;

import com.hs.railway_stats.repository.entity.TripInfoMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TripInfoMetricRepository extends JpaRepository<TripInfoMetric, Integer> {

    Optional<TripInfoMetric> findByOriginIdAndDestinationIdAndScheduledDepartureTime(
            int originId, int destinationId, LocalTime scheduledDepartureTime);

    @Transactional
    @Modifying
    @Query("UPDATE TripInfoMetric m SET m.totalReimbursementsRequested = m.totalReimbursementsRequested + 1 " +
           "WHERE m.originId = :originId AND m.destinationId = :destinationId " +
           "AND m.scheduledDepartureTime = :scheduledDepartureTime")
    int incrementReimbursementsRequested(int originId, int destinationId, LocalTime scheduledDepartureTime);

    @Query("SELECT DISTINCT m FROM TripInfoMetric m LEFT JOIN FETCH m.canceledTripDates " +
           "WHERE m.originId = :originId AND m.destinationId = :destinationId")
    List<TripInfoMetric> findByOriginIdAndDestinationId(int originId, int destinationId);
}
