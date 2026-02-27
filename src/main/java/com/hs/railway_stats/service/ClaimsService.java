package com.hs.railway_stats.service;

import com.hs.railway_stats.dto.ClaimRequest;

public interface ClaimsService {
    void submitClaim(ClaimRequest request);
}
