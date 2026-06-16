package com.trao.tripplanner.service.strategy;

import com.trao.tripplanner.model.Trip;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

// Pattern-Strategy: Concrete strategy — generates rich, detailed itineraries with timings and costs
// Pattern-Singleton: @Component makes this a Spring-managed singleton
// SOLID-LSP: Fully honors the ItineraryGenerationStrategy contract, so TripService can swap in
// any other implementation (e.g. a future "budget-only" or "minimal" strategy) with no caller changes
@Component("detailedItineraryStrategy")
public class DetailedItineraryStrategy implements ItineraryGenerationStrategy {

    @Override
    public String buildItineraryPrompt(Trip trip) {
        String interests = String.join(", ", trip.getInterests());
        return String.format("""
            You are an expert travel planner. Generate a detailed %d-day itinerary for %s.

            Traveler preferences:
            - Budget level: %s
            - Interests: %s

            IMPORTANT: Respond ONLY with a valid JSON object. No markdown, no explanation.

            Required JSON structure:
            {
              "days": [
                {
                  "dayNumber": 1,
                  "theme": "Arrival & Cultural Immersion",
                  "activities": [
                    {
                      "title": "Activity name",
                      "description": "Detailed description of the activity",
                      "timeOfDay": "Morning|Afternoon|Evening",
                      "location": "Specific address or area",
                      "estimatedDuration": "2 hours",
                      "estimatedCost": "$20",
                      "orderIndex": 0
                    }
                  ]
                }
              ],
              "budgetEstimate": {
                "flights": 400,
                "accommodation": 300,
                "food": 150,
                "activities": 100,
                "total": 950
              }
            }

            Rules:
            - Include 3-4 activities per day
            - Match budget level (%s): LOW=budget options, MEDIUM=mid-range, HIGH=luxury
            - Tailor activities to interests: %s
            - Include specific, real locations
            - Vary time of day across activities
            """,
                trip.getNumberOfDays(), trip.getDestination(),
                trip.getBudgetType(), interests,
                trip.getBudgetType(), interests);
    }

    @Override
    public String buildDayRegenerationPrompt(Trip trip, int dayNumber, String instruction) {
        String interests = String.join(", ", trip.getInterests());
        return String.format("""
            You are an expert travel planner. Regenerate Day %d of a %d-day trip to %s.

            User request: "%s"
            Budget level: %s
            Interests: %s

            IMPORTANT: Respond ONLY with a valid JSON object for a single day.

            Required JSON structure:
            {
              "dayNumber": %d,
              "theme": "Theme for the day",
              "activities": [
                {
                  "title": "Activity name",
                  "description": "Detailed description",
                  "timeOfDay": "Morning|Afternoon|Evening",
                  "location": "Specific location",
                  "estimatedDuration": "2 hours",
                  "estimatedCost": "$20",
                  "orderIndex": 0
                }
              ]
            }
            """,
                dayNumber, trip.getNumberOfDays(), trip.getDestination(),
                instruction, trip.getBudgetType(), interests, dayNumber);
    }

    @Override
    public String buildHotelSuggestionsPrompt(Trip trip) {
        return String.format("""
            You are a travel accommodation expert. Suggest hotels for %s.

            Budget level: %s
            Trip duration: %d nights

            IMPORTANT: Respond ONLY with a valid JSON object.

            Required JSON structure:
            {
              "hotels": [
                {
                  "name": "Hotel name",
                  "category": "Budget|Mid-Range|Luxury",
                  "pricePerNight": "$80",
                  "rating": 4.2,
                  "highlights": ["Free WiFi", "City center", "Pool"],
                  "bookingTip": "Book 2 weeks in advance for best rates"
                }
              ]
            }

            Provide 3 hotels: one budget, one mid-range, one luxury.
            Focus on hotels matching the %s budget preference most prominently.
            """,
                trip.getDestination(), trip.getBudgetType(),
                trip.getNumberOfDays(), trip.getBudgetType());
    }

    @Override
    public String getStrategyName() {
        return "DETAILED";
    }
}
