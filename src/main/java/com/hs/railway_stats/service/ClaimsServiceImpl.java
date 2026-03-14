package com.hs.railway_stats.service;

import com.hs.railway_stats.dto.ClaimRequest;
import com.hs.railway_stats.exception.ClaimSubmissionException;
import com.hs.railway_stats.exception.StationNotFoundException;
import com.hs.railway_stats.external.RestClient;
import com.hs.railway_stats.repository.TripInfoMetricRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.LocalTime;

@Service
public class ClaimsServiceImpl implements ClaimsService {

    private static final Logger log = LoggerFactory.getLogger(ClaimsServiceImpl.class);

    private final RestClient restClient;
    private final TripInfoMetricRepository tripInfoMetricRepository;
    private final TranslationService translationService;

    public ClaimsServiceImpl(RestClient restClient, TripInfoMetricRepository tripInfoMetricRepository, TranslationService translationService) {
        this.restClient = restClient;
        this.tripInfoMetricRepository = tripInfoMetricRepository;
        this.translationService = translationService;
    }

    @Override
    public void submitClaim(ClaimRequest request) {
        log.info("Submitting claim for ticketNumber={}, departureStationId={}, arrivalStationId={}",
                request.ticketNumber(), request.departureStationId(), request.arrivalStationId());
        try {
            restClient.callClaim(request);
            try {
                int originId = translationService.translateClaimsStationId(request.departureStationId());
                int destinationId = translationService.translateClaimsStationId(request.arrivalStationId());
                LocalTime scheduledDepartureTime = OffsetDateTime.parse(request.departureDate()).toLocalTime();
                incrementReimbursementCount(originId, destinationId, scheduledDepartureTime);
            } catch (StationNotFoundException e) {
                log.error("Could not update reimbursement count — station not found: {}", e.getMessage());
            }
        } catch (ClaimSubmissionException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("REST call failed for ticketNumber={}: {}", request.ticketNumber(), ex.getMessage(), ex);
            throw new ClaimSubmissionException("Failed to submit claim", ex, false);
        }
    }

    private void incrementReimbursementCount(int originId, int destinationId, LocalTime scheduledDepartureTime) {
        int updated = tripInfoMetricRepository.incrementReimbursementsRequested(originId, destinationId, scheduledDepartureTime);
        if (updated == 0) {
            log.warn("No metric found to increment reimbursement count for originId={}, destinationId={}, departure={}",
                    originId, destinationId, scheduledDepartureTime);
        } else {
            log.debug("Incremented reimbursement count for originId={}, destinationId={}, departure={}",
                    originId, destinationId, scheduledDepartureTime);
        }
    }
}
