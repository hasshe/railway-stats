package com.hs.railway_stats.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hs.railway_stats.dto.TripInfoResponse;
import com.hs.railway_stats.service.TripInfoService;


@RestController
@RequestMapping("/trip-info")
public class AdminController {

    private final TripInfoService demoService;

    public AdminController(TripInfoService demoService) {
        this.demoService = demoService;
    }

    @GetMapping("/{originId}/{destinationId}")
    public List<TripInfoResponse> getTripInformation(@PathVariable long originId, @PathVariable long destinationId) {
        return demoService.getTripInfo(originId, destinationId);
    }
    
}
