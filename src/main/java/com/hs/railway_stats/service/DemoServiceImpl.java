package com.hs.railway_stats.service;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;

import com.hs.railway_stats.external.RestClient;
import com.hs.railway_stats.external.TripResponse;

@Service
public class DemoServiceImpl implements DemoService {

    private RestClient restClient;

    public DemoServiceImpl(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public String getDemoString(String param) {
        try {
        TripResponse resp = restClient.callSearch(OffsetDateTime.now(), 740000005, 740000001, null);
        return "Hello, " + param + "! Response: " + resp.trips().getFirst();
        } catch (Exception e) {
            return "Hello, " + param + "! An error occurred: " + e.getMessage();
        }
    }
    
}
