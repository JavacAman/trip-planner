package com.trao.tripplanner.service.strategy;

import com.trao.tripplanner.model.Trip;
import com.trao.tripplanner.model.Trip.BudgetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DetailedItineraryStrategy Unit Tests")
class DetailedItineraryStrategyTest {

    private DetailedItineraryStrategy strategy;
    private Trip testTrip;

    @BeforeEach
    void setUp() {
        strategy = new DetailedItineraryStrategy();
        testTrip = Trip.builder()
                .id(1L)
                .destination("Paris, France")
                .numberOfDays(5)
                .budgetType(BudgetType.HIGH)
                .interests(List.of("Culture", "Food", "Art"))
                .itinerary(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("buildItineraryPrompt: contains destination, days, and budget info")
    void buildItineraryPrompt_containsKeyInfo() {
        String prompt = strategy.buildItineraryPrompt(testTrip);

        assertThat(prompt).contains("Paris, France");
        assertThat(prompt).contains("5");
        assertThat(prompt).contains("HIGH");
        assertThat(prompt).contains("Culture");
        assertThat(prompt).contains("JSON");
    }

    @Test
    @DisplayName("buildDayRegenerationPrompt: includes day number and user instruction")
    void buildDayRegenerationPrompt_containsDayAndInstruction() {
        String prompt = strategy.buildDayRegenerationPrompt(testTrip, 3, "Add more outdoor activities");

        assertThat(prompt).contains("Day 3");
        assertThat(prompt).contains("Add more outdoor activities");
        assertThat(prompt).contains("Paris, France");
    }

    @Test
    @DisplayName("buildHotelSuggestionsPrompt: includes destination and budget type")
    void buildHotelSuggestionsPrompt_containsDestinationAndBudget() {
        String prompt = strategy.buildHotelSuggestionsPrompt(testTrip);

        assertThat(prompt).contains("Paris, France");
        assertThat(prompt).contains("HIGH");
        assertThat(prompt).contains("JSON");
    }

    @Test
    @DisplayName("getStrategyName: returns DETAILED")
    void getStrategyName_returnsCorrectName() {
        assertThat(strategy.getStrategyName()).isEqualTo("DETAILED");
    }
}
