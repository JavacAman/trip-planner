package com.trao.tripplanner.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

// SOLID-SRP: Handles only activity creation/update input
@Data
public class ActivityRequest {

    @NotBlank(message = "Activity title is required")
    private String title;

    private String description;

    private String timeOfDay;

    private String location;

    private String estimatedDuration;

    private String estimatedCost;

    @NotNull(message = "Day number is required")
    private Integer dayNumber;
}
