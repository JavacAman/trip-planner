package com.trao.tripplanner.dto.request;

import com.trao.tripplanner.model.Trip.BudgetType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

// SOLID-SRP: Handles only trip creation input contract and validation
@Data
public class TripRequest {

    @NotBlank(message = "Destination is required")
    @Size(min = 2, max = 200, message = "Destination must be between 2 and 200 characters")
    private String destination;

    @NotNull(message = "Number of days is required")
    @Min(value = 1, message = "Trip must be at least 1 day")
    @Max(value = 30, message = "Trip cannot exceed 30 days")
    private Integer numberOfDays;

    @NotNull(message = "Budget type is required")
    private BudgetType budgetType;

    @NotEmpty(message = "At least one interest is required")
    @Size(max = 10, message = "Cannot have more than 10 interests")
    private List<String> interests;
}
