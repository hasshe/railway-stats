package com.hs.railway_stats.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.hs.railway_stats.dto.TripInfoResponse;
import com.hs.railway_stats.dto.TripResponse;
import com.hs.railway_stats.external.RestClient;
import com.hs.railway_stats.mapper.TripInfoMapper;

@Service
public class DemoServiceImpl implements DemoService {

    private RestClient restClient;
    public DemoServiceImpl(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<TripInfoResponse> getDemoList(String param) {
        try {
        TripResponse resp = restClient.callSearch(740000005, 740000001, null);
    
        return TripInfoMapper.mapFromTripResponse(resp);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch trip info", e);
        }
    }
    
}
