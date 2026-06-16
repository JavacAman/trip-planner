package com.trao.tripplanner.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trao.tripplanner.exception.AiServiceException;
import com.trao.tripplanner.model.DayItinerary;
import com.trao.tripplanner.model.Trip;
import com.trao.tripplanner.model.Trip.BudgetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItineraryParserService Unit Tests")
class ItineraryParserServiceTest {

    private ItineraryParserService parserService;
    private Trip testTrip;

    @BeforeEach
    void setUp() {
        parserService = new ItineraryParserService(new ObjectMapper());
        testTrip = Trip.builder()
                .id(1L)
                .destination("Tokyo")
                .numberOfDays(2)
                .budgetType(BudgetType.MEDIUM)
                .interests(List.of("Culture"))
                .itinerary(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("parseItinerary: parses valid JSON into DayItinerary list")
    void parseItinerary_validJson_returnsCorrectDays() {
        String json = """
            {
              "days": [
                {
                  "dayNumber": 1,
                  "theme": "Cultural Exploration",
                  "activities": [
                    {
                      "title": "Visit Senso-ji Temple",
                      "description": "Ancient Buddhist temple",
                      "timeOfDay": "Morning",
                      "location": "Asakusa, Tokyo",
                      "estimatedDuration": "2 hours",
                      "estimatedCost": "$0",
                      "orderIndex": 0
                    }
                  ]
                }
              ]
            }
            """;

        List<DayItinerary> days = parserService.parseItinerary(json, testTrip);

        assertThat(days).hasSize(1);
        assertThat(days.get(0).getDayNumber()).isEqualTo(1);
        assertThat(days.get(0).getTheme()).isEqualTo("Cultural Exploration");
        assertThat(days.get(0).getActivities()).hasSize(1);
        assertThat(days.get(0).getActivities().get(0).getTitle()).isEqualTo("Visit Senso-ji Temple");
    }

    @Test
    @DisplayName("parseItinerary: throws AiServiceException on malformed JSON")
    void parseItinerary_invalidJson_throwsException() {
        assertThatThrownBy(() -> parserService.parseItinerary("not-json", testTrip))
                .isInstanceOf(AiServiceException.class);
    }

    @Test
    @DisplayName("parseBudgetEstimate: correctly parses numeric budget values into Trip")
    void parseBudgetEstimate_validJson_setsCorrectValues() {
        String json = """
            {
              "days": [],
              "budgetEstimate": {
                "flights": 400,
                "accommodation": 300,
                "food": 150,
                "activities": 100,
                "total": 950
              }
            }
            """;

        parserService.parseBudgetEstimate(json, testTrip);

        assertThat(testTrip.getEstimatedFlightCost()).isEqualByComparingTo(new BigDecimal("400"));
        assertThat(testTrip.getTotalEstimatedBudget()).isEqualByComparingTo(new BigDecimal("950"));
    }

    @Test
    @DisplayName("parseBudgetEstimate: does not throw when budgetEstimate is missing")
    void parseBudgetEstimate_missingSection_noException() {
        String json = "{\"days\": []}";
        // Should not throw — gracefully skips missing budget
        parserService.parseBudgetEstimate(json, testTrip);
        assertThat(testTrip.getTotalEstimatedBudget()).isNull();
    }
}
