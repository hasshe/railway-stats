package com.hs.railway_stats.service;

import com.hs.railway_stats.dto.ClaimRequest;
import com.hs.railway_stats.external.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClaimsServiceImpl implements ClaimsService {
    private final RestClient restClient;

    @Autowired
    public ClaimsServiceImpl(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void submitClaim(ClaimRequest request) {
        try {
            restClient.callClaim(request);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to submit claim: " + ex.getMessage(), ex);
        }
    }
}
