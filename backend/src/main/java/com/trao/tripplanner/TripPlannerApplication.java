package com.trao.tripplanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

// Pattern-Singleton: Spring's @SpringBootApplication ensures a single application context
@SpringBootApplication
@EnableCaching
@EnableAsync
public class TripPlannerApplication {
    public static void main(String[] args) {
        SpringApplication.run(TripPlannerApplication.class, args);
    }
}
