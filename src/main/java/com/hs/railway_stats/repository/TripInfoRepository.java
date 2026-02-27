package com.hs.railway_stats.repository;

import com.hs.railway_stats.repository.entity.TripInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface TripInfoRepository extends JpaRepository<TripInfo, Integer> {

    @Query("SELECT t FROM TripInfo t WHERE t.originId = :originId AND t.destinationId = :destinationId " +
           "AND t.originalDepartureTime >= :startOfDay AND t.originalDepartureTime < :endOfDay")
    List<TripInfo> findByOriginAndDestinationAndDate(int originId, int destinationId,
                                                      ZonedDateTime startOfDay, ZonedDateTime endOfDay);

    @Modifying
    @Query("DELETE FROM TripInfo t WHERE t.originalDepartureTime >= :startOfDay AND t.originalDepartureTime < :endOfDay")
    void deleteByDate(ZonedDateTime startOfDay, ZonedDateTime endOfDay);
}
