package com.trao.tripplanner.dto.response;

import com.trao.tripplanner.model.Trip.BudgetType;
import com.trao.tripplanner.model.Trip.TripStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Pattern-Factory: Assembled by TripResponseFactory to map from domain model
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripResponse {
    private Long id;
    private String destination;
    private Integer numberOfDays;
    private BudgetType budgetType;
    private List<String> interests;
    private List<DayItineraryResponse> itinerary;
    private BigDecimal estimatedFlightCost;
    private BigDecimal estimatedAccommodationCost;
    private BigDecimal estimatedFoodCost;
    private BigDecimal estimatedActivitiesCost;
    private BigDecimal totalEstimatedBudget;
    private TripStatus status;
    private String hotelSuggestions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
