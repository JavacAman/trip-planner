package com.trao.tripplanner.service.strategy;

import com.trao.tripplanner.model.Trip;

// Pattern-Strategy: Defines the contract for swappable itinerary generation algorithms
// SOLID-OCP: New generation strategies (e.g., GPT-based, template-based) can be added without modifying callers
// SOLID-DIP: Callers depend on this abstraction, not concrete implementations
public interface ItineraryGenerationStrategy {

    /**
     * Builds the AI prompt for generating a full trip itinerary.
     * Different strategies produce different prompt styles (detailed, concise, theme-focused, etc.)
     */
    String buildItineraryPrompt(Trip trip);

    /**
     * Builds the AI prompt for regenerating a specific day.
     */
    String buildDayRegenerationPrompt(Trip trip, int dayNumber, String instruction);

    /**
     * Builds the AI prompt for hotel suggestions.
     */
    String buildHotelSuggestionsPrompt(Trip trip);

    /**
     * Returns the strategy identifier for logging and cache-key namespacing.
     */
    String getStrategyName();
}
