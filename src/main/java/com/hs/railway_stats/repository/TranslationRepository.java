package com.hs.railway_stats.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.hs.railway_stats.repository.entity.Translation;

@Repository
public interface TranslationRepository extends JpaRepository<Translation, Integer> {
    @Query("SELECT t FROM Translation t WHERE LOWER(t.stationName) = LOWER(?1)")
    Optional<Translation> findByStationName(String stationName);
    Optional<Translation> findByStationId(int stationId);
}
