package com.hs.railway_stats.service;

import com.hs.railway_stats.dto.ClaimRequest;
import com.hs.railway_stats.external.RestClient;
import com.hs.railway_stats.repository.TripInfoMetricRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

@Service
public class ClaimsServiceImpl implements ClaimsService {

    private static final Logger log = LoggerFactory.getLogger(ClaimsServiceImpl.class);

    private final RestClient restClient;
    private final TripInfoMetricRepository tripInfoMetricRepository;

    @Autowired
    public ClaimsServiceImpl(RestClient restClient, TripInfoMetricRepository tripInfoMetricRepository) {
        this.restClient = restClient;
        this.tripInfoMetricRepository = tripInfoMetricRepository;
    }

    @Override
    public void submitClaim(ClaimRequest request) {
        log.info("ClaimsService: calling REST endpoint for ticketNumber={}, departureStationId={}, arrivalStationId={}",
                request.ticketNumber(), request.departureStationId(), request.arrivalStationId());
        try {
            restClient.callClaim(request);
            try {
                int originId = Integer.parseInt(request.departureStationId());
                int destinationId = Integer.parseInt(request.arrivalStationId());
                String timeStr = request.departureDate().substring(11, 16);
                LocalTime scheduledDepartureTime = LocalTime.parse(timeStr);
                updateMetric(originId, destinationId, scheduledDepartureTime);
            } catch (Exception e) {
                log.error("Failed to update totalReimbursementsRequested: {}", e.getMessage(), e);
            }
        } catch (Exception ex) {
            log.error("ClaimsService: REST call failed for ticketNumber={}: {}", request.ticketNumber(), ex.getMessage(), ex);
            throw new RuntimeException("Failed to submit claim: " + ex.getMessage(), ex);
        }
    }

    private void updateMetric(int originId, int destinationId, LocalTime scheduledDepartureTime) {
        tripInfoMetricRepository.findByOriginIdAndDestinationIdAndScheduledDepartureTime(
                originId, destinationId, scheduledDepartureTime
        ).ifPresent(metric -> {
            metric.setTotalReimbursementsRequested(metric.getTotalReimbursementsRequested() + 1);
            tripInfoMetricRepository.save(metric);
            log.info("Updated totalReimbursementsRequested for metric id={}", metric.getId());
        });
    }
}
