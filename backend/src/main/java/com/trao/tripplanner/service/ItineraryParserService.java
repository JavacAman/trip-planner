package com.trao.tripplanner.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trao.tripplanner.exception.AiServiceException;
import com.trao.tripplanner.model.Activity;
import com.trao.tripplanner.model.DayItinerary;
import com.trao.tripplanner.model.Trip;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// SOLID-SRP: Handles only parsing of AI JSON responses into domain model objects
@Service
@RequiredArgsConstructor
@Slf4j
public class ItineraryParserService {

    private final ObjectMapper objectMapper;

    public List<DayItinerary> parseItinerary(String jsonContent, Trip trip) {
        try {
            JsonNode root = objectMapper.readTree(jsonContent);
            JsonNode daysNode = root.path("days");

            List<DayItinerary> days = new ArrayList<>();
            for (JsonNode dayNode : daysNode) {
                DayItinerary day = parseSingleDay(dayNode, trip);
                days.add(day);
            }
            return days;
        } catch (Exception ex) {
            log.error("Failed to parse itinerary JSON: {}", ex.getMessage());
            throw new AiServiceException("Failed to parse itinerary from AI response");
        }
    }

    public DayItinerary parseSingleDay(String jsonContent, Trip trip) {
        try {
            JsonNode dayNode = objectMapper.readTree(jsonContent);
            return parseSingleDay(dayNode, trip);
        } catch (Exception ex) {
            log.error("Failed to parse day JSON: {}", ex.getMessage());
            throw new AiServiceException("Failed to parse day from AI response");
        }
    }

    private DayItinerary parseSingleDay(JsonNode dayNode, Trip trip) {
        DayItinerary day = DayItinerary.builder()
                .trip(trip)
                .dayNumber(dayNode.path("dayNumber").asInt())
                .theme(dayNode.path("theme").asText(""))
                .activities(new ArrayList<>())
                .build();

        JsonNode activitiesNode = dayNode.path("activities");
        int orderIndex = 0;
        for (JsonNode actNode : activitiesNode) {
            Activity activity = Activity.builder()
                    .dayItinerary(day)
                    .title(actNode.path("title").asText(""))
                    .description(actNode.path("description").asText(""))
                    .timeOfDay(actNode.path("timeOfDay").asText(""))
                    .location(actNode.path("location").asText(""))
                    .estimatedDuration(actNode.path("estimatedDuration").asText(""))
                    .estimatedCost(actNode.path("estimatedCost").asText(""))
                    .orderIndex(orderIndex++)
                    .build();
            day.getActivities().add(activity);
        }
        return day;
    }

    public void parseBudgetEstimate(String jsonContent, Trip trip) {
        try {
            JsonNode root = objectMapper.readTree(jsonContent);
            JsonNode budget = root.path("budgetEstimate");

            if (!budget.isMissingNode()) {
                trip.setEstimatedFlightCost(parseCost(budget.path("flights")));
                trip.setEstimatedAccommodationCost(parseCost(budget.path("accommodation")));
                trip.setEstimatedFoodCost(parseCost(budget.path("food")));
                trip.setEstimatedActivitiesCost(parseCost(budget.path("activities")));
                trip.setTotalEstimatedBudget(parseCost(budget.path("total")));
            }
        } catch (Exception ex) {
            log.warn("Could not parse budget estimate, skipping: {}", ex.getMessage());
        }
    }

    private BigDecimal parseCost(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) return BigDecimal.ZERO;
        // Handle both numeric and string formats like "$400"
        String text = node.isNumber() ? node.asText() : node.asText().replaceAll("[^0-9.]", "");
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
