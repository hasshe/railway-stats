package com.hs.railway_stats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RailwayStatsApplication {

	void main() {
		SpringApplication.run(RailwayStatsApplication.class);
	}
}
