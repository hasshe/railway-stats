package com.hs.railway_stats.service;

import com.hs.railway_stats.repository.TripInfoRepository;
import com.hs.railway_stats.util.DateRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
public class TripPruningService {

    private static final Logger logger = LoggerFactory.getLogger(TripPruningService.class);

    private final TripInfoRepository tripInfoRepository;

    @Value("${tripinfo.scheduling.zone}")
    private String zoneId;

    @Value("${tripinfo.retention.days}")
    private int retentionDays;

    public TripPruningService(TripInfoRepository tripInfoRepository) {
        this.tripInfoRepository = tripInfoRepository;
    }

    @Transactional
    public void pruneOldTrips() {
        ZoneId zone = ZoneId.of(zoneId);
        LocalDate today = LocalDate.now(zone);
        LocalDate cutoffDate = today.minusDays(retentionDays);
        ZonedDateTime cutoff = cutoffDate.atStartOfDay(zone);

        long daysBeforeCutoff = tripInfoRepository.countDistinctDaysBefore(cutoff);
        if (daysBeforeCutoff == 0) {
            logger.info("Rolling-window pruning: no data older than {} days, nothing to remove", retentionDays);
            return;
        }

        tripInfoRepository.findEarliestDepartureTime().ifPresent(earliest -> {
            LocalDate oldestDay = earliest.withZoneSameInstant(zone).toLocalDate();
            if (!oldestDay.isBefore(cutoffDate)) {
                logger.info("Rolling-window pruning: oldest day {} is within the {}-day window, nothing to remove",
                        oldestDay, retentionDays);
                return;
            }
            DateRange range = DateRange.ofDay(oldestDay, zone);
            tripInfoRepository.deleteByDate(range.start(), range.end());
            logger.info("Rolling-window pruning: removed trip records for {} (older than {} days)", oldestDay, retentionDays);
        });
    }
}

