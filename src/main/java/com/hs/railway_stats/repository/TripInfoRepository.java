package com.hs.railway_stats.repository;

import com.hs.railway_stats.repository.entity.TripInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TripInfoRepository extends JpaRepository<TripInfo, Integer> {
}
