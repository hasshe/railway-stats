package com.hs.railway_stats.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hs.railway_stats.dto.TripInfoResponse;
import com.hs.railway_stats.service.DemoService;


@RestController
@RequestMapping("/demo")
public class DemoController {

    private final DemoService demoService;

    public DemoController(DemoService demoService) {
        this.demoService = demoService;
    }

    @GetMapping
    public List<TripInfoResponse> getMethodName(@RequestParam String param) {
        return demoService.getDemoList(param);
    }
    
}
