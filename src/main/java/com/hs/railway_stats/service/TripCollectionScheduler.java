package com.hs.railway_stats.service;

import com.hs.railway_stats.config.StationConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TripCollectionScheduler {

    private static final Logger logger = LoggerFactory.getLogger(TripCollectionScheduler.class);

    private final TripInfoService tripInfoService;
    private final TripPruningService tripPruningService;
    private final TripInfoMetricService tripInfoMetricService;

    public TripCollectionScheduler(TripInfoService tripInfoService,
                                   TripPruningService tripPruningService,
                                   TripInfoMetricService tripInfoMetricService) {
        this.tripInfoService = tripInfoService;
        this.tripPruningService = tripPruningService;
        this.tripInfoMetricService = tripInfoMetricService;
    }

    @Scheduled(cron = "${tripinfo.scheduling.collect-cron}", zone = "${tripinfo.scheduling.zone}")
    public void scheduleCollection() {
        logger.info("Starting scheduled trip information collection job");
        tripInfoService.collectTripInformation(StationConstants.UPPSALA, StationConstants.STOCKHOLM);
        tripInfoService.collectTripInformation(StationConstants.STOCKHOLM, StationConstants.UPPSALA);
        logger.info("Scheduled trip information collection job completed successfully");
    }

    @Scheduled(cron = "${tripinfo.scheduling.prune-cron}", zone = "${tripinfo.scheduling.zone}")
    public void schedulePruning() {
        tripPruningService.pruneOldTrips();
    }

    @Scheduled(cron = "${tripinfo.scheduling.metrics-refresh-cron}", zone = "${tripinfo.scheduling.zone}")
    public void scheduleMetricsRefresh() {
        tripInfoMetricService.refreshMetricsCache(StationConstants.ALL_STATIONS);
    }
}
