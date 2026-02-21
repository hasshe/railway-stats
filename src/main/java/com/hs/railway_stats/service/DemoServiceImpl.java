package com.hs.railway_stats.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.hs.railway_stats.external.RestClient;
import com.hs.railway_stats.external.dto.TripResponse;

@Service
public class DemoServiceImpl implements DemoService {

    private RestClient restClient;

    public DemoServiceImpl(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<String> getDemoList(String param) {
        try {
        TripResponse resp = restClient.callSearch(740000005, 740000001, null);
        return resp.trips().stream()
                .map(e -> e.legs().getFirst().origin().name() + " to " + e.legs().getFirst().destination().name()).toList();
        } catch (Exception e) {
            return List.of("Hello, " + param + "! An error occurred: " + e.getMessage());
        }
    }
    
}
