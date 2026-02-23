package com.hs.railway_stats.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hs.railway_stats.repository.entity.TripInfo;

@Repository
public interface TripInfoRepository extends JpaRepository<TripInfo, Integer> {
}
