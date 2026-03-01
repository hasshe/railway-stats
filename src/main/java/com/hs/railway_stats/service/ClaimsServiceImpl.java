package com.hs.railway_stats.service;

import com.hs.railway_stats.dto.ClaimRequest;
import com.hs.railway_stats.external.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClaimsServiceImpl implements ClaimsService {

    private static final Logger log = LoggerFactory.getLogger(ClaimsServiceImpl.class);

    private final RestClient restClient;

    @Autowired
    public ClaimsServiceImpl(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void submitClaim(ClaimRequest request) {
        log.info("ClaimsService: calling REST endpoint for ticketNumber={}, departureStationId={}, arrivalStationId={}",
                request.ticketNumber(), request.departureStationId(), request.arrivalStationId());
        try {
            restClient.callClaim(request);
        } catch (Exception ex) {
            log.error("ClaimsService: REST call failed for ticketNumber={}: {}", request.ticketNumber(), ex.getMessage(), ex);
            throw new RuntimeException("Failed to submit claim: " + ex.getMessage(), ex);
        }
    }
}
