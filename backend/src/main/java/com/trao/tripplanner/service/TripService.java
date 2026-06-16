package com.trao.tripplanner.service;

import com.trao.tripplanner.dto.request.ActivityRequest;
import com.trao.tripplanner.dto.request.ModifyDayRequest;
import com.trao.tripplanner.dto.request.TripRequest;
import com.trao.tripplanner.dto.response.*;
import com.trao.tripplanner.exception.TripNotFoundException;
import com.trao.tripplanner.model.*;
import com.trao.tripplanner.repository.DayItineraryRepository;
import com.trao.tripplanner.repository.TripRepository;
import com.trao.tripplanner.repository.UserRepository;
import com.trao.tripplanner.service.observer.TripEvent;
import com.trao.tripplanner.service.observer.TripEvent.TripEventType;
import com.trao.tripplanner.service.strategy.ItineraryGenerationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// SOLID-SRP: Orchestrates trip lifecycle — delegates AI, parsing, caching to dedicated services
// SOLID-DIP: Depends on ItineraryGenerationStrategy abstraction, not a concrete AI implementation
// ACID-Atomicity: @Transactional ensures trip creation + itinerary persistence are atomic
@Service
@RequiredArgsConstructor
@Slf4j
public class TripService {

    private final TripRepository tripRepository;
    private final DayItineraryRepository dayItineraryRepository;
    private final UserRepository userRepository;
    private final GrokAiService grokAiService;
    private final ItineraryGenerationStrategy itineraryGenerationStrategy;
    private final ItineraryParserService parserService;
    private final ApplicationEventPublisher eventPublisher;

    // ACID-Atomicity: Rolls back if AI call succeeds but DB save fails
    @Transactional
    @CacheEvict(value = "trip_cache", key = "#userEmail")
    public TripResponse createTrip(TripRequest request, String userEmail) {
        User user = loadUser(userEmail);

        // Pattern-Builder: Builder pattern for Trip construction
        Trip trip = Trip.builder()
                .user(user)
                .destination(request.getDestination())
                .numberOfDays(request.getNumberOfDays())
                .budgetType(request.getBudgetType())
                .interests(request.getInterests())
                .status(Trip.TripStatus.GENERATING)
                .build();

        trip = tripRepository.save(trip);

        // Pattern-Observer: Publish event after trip is created
        eventPublisher.publishEvent(new TripEvent(this, trip, TripEventType.TRIP_CREATED));

        try {
            // Pattern-Strategy: Use injected strategy to build the prompt
            String prompt = itineraryGenerationStrategy.buildItineraryPrompt(trip);
            String aiResponse = grokAiService.generateContent(prompt);

            List<DayItinerary> days = parserService.parseItinerary(aiResponse, trip);
            parserService.parseBudgetEstimate(aiResponse, trip);

            trip.getItinerary().clear();
            trip.getItinerary().addAll(days);
            trip.setStatus(Trip.TripStatus.COMPLETED);

            // Generate hotel suggestions asynchronously using the strategy
            String hotelPrompt = itineraryGenerationStrategy.buildHotelSuggestionsPrompt(trip);
            String hotelResponse = grokAiService.generateContent(hotelPrompt);
            trip.setHotelSuggestions(hotelResponse);

            trip = tripRepository.save(trip);

            // Pattern-Observer: Publish itinerary generated event
            eventPublisher.publishEvent(new TripEvent(this, trip, TripEventType.ITINERARY_GENERATED));

        } catch (Exception ex) {
            trip.setStatus(Trip.TripStatus.FAILED);
            tripRepository.save(trip);
            log.error("Failed to generate itinerary for tripId={}: {}", trip.getId(), ex.getMessage());
            throw ex;
        }

        return mapToResponse(trip);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "trip_cache", key = "#userEmail", condition = "#pageable.pageNumber == 0")
    public Page<TripResponse> getUserTrips(String userEmail, Pageable pageable) {
        User user = loadUser(userEmail);
        return tripRepository.findByUserId(user.getId(), pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "itinerary_cache", key = "#tripId")
    public TripResponse getTripById(Long tripId, String userEmail) {
        User user = loadUser(userEmail);
        Trip trip = tripRepository.findByIdAndUserId(tripId, user.getId())
                .orElseThrow(() -> new TripNotFoundException(tripId));
        return mapToResponse(trip);
    }

    // ACID-Atomicity: Entire day regeneration is atomic — fails together or succeeds together
    @Transactional
    @CacheEvict(value = "itinerary_cache", key = "#tripId")
    public TripResponse regenerateDay(Long tripId, Integer dayNumber,
                                      ModifyDayRequest request, String userEmail) {
        User user = loadUser(userEmail);
        Trip trip = tripRepository.findByIdAndUserId(tripId, user.getId())
                .orElseThrow(() -> new TripNotFoundException(tripId));

        // Pattern-Strategy: Delegate prompt construction to strategy
        String prompt = itineraryGenerationStrategy.buildDayRegenerationPrompt(trip, dayNumber, request.getInstruction());
        String aiResponse = grokAiService.generateContent(prompt);

        DayItinerary newDay = parserService.parseSingleDay(aiResponse, trip);

        // Replace the existing day in-place
        trip.getItinerary().removeIf(d -> d.getDayNumber().equals(dayNumber));
        trip.getItinerary().add(newDay);

        trip = tripRepository.save(trip);

        // Pattern-Observer: Publish modification event
        eventPublisher.publishEvent(new TripEvent(this, trip, TripEventType.DAY_REGENERATED));

        return mapToResponse(trip);
    }

    @Transactional
    @CacheEvict(value = "itinerary_cache", key = "#tripId")
    public TripResponse addActivity(Long tripId, ActivityRequest request, String userEmail) {
        User user = loadUser(userEmail);
        Trip trip = tripRepository.findByIdAndUserId(tripId, user.getId())
                .orElseThrow(() -> new TripNotFoundException(tripId));

        DayItinerary day = dayItineraryRepository
                .findByTripIdAndDayNumber(tripId, request.getDayNumber())
                .orElseThrow(() -> new TripNotFoundException(tripId));

        int nextOrder = day.getActivities().size();
        Activity activity = Activity.builder()
                .dayItinerary(day)
                .title(request.getTitle())
                .description(request.getDescription())
                .timeOfDay(request.getTimeOfDay())
                .location(request.getLocation())
                .estimatedDuration(request.getEstimatedDuration())
                .estimatedCost(request.getEstimatedCost())
                .orderIndex(nextOrder)
                .build();

        day.getActivities().add(activity);
        tripRepository.save(trip);

        eventPublisher.publishEvent(new TripEvent(this, trip, TripEventType.ITINERARY_MODIFIED));
        return mapToResponse(trip);
    }

    @Transactional
    @CacheEvict(value = "itinerary_cache", key = "#tripId")
    public TripResponse removeActivity(Long tripId, Long activityId, String userEmail) {
        User user = loadUser(userEmail);
        Trip trip = tripRepository.findByIdAndUserId(tripId, user.getId())
                .orElseThrow(() -> new TripNotFoundException(tripId));

        trip.getItinerary().forEach(day ->
                day.getActivities().removeIf(a -> a.getId().equals(activityId)));

        trip = tripRepository.save(trip);
        eventPublisher.publishEvent(new TripEvent(this, trip, TripEventType.ITINERARY_MODIFIED));
        return mapToResponse(trip);
    }

    @Transactional
    @CacheEvict(value = {"trip_cache", "itinerary_cache"}, allEntries = true)
    public void deleteTrip(Long tripId, String userEmail) {
        User user = loadUser(userEmail);
        if (!tripRepository.existsByIdAndUserId(tripId, user.getId())) {
            throw new TripNotFoundException(tripId);
        }
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new TripNotFoundException(tripId));
        eventPublisher.publishEvent(new TripEvent(this, trip, TripEventType.TRIP_DELETED));
        tripRepository.deleteById(tripId);
    }

    // Pattern-Factory: Centralizes mapping from domain model to response DTO
    private TripResponse mapToResponse(Trip trip) {
        List<DayItineraryResponse> dayResponses = trip.getItinerary().stream()
                .map(day -> DayItineraryResponse.builder()
                        .id(day.getId())
                        .dayNumber(day.getDayNumber())
                        .theme(day.getTheme())
                        .activities(day.getActivities().stream()
                                .map(act -> ActivityResponse.builder()
                                        .id(act.getId())
                                        .title(act.getTitle())
                                        .description(act.getDescription())
                                        .timeOfDay(act.getTimeOfDay())
                                        .location(act.getLocation())
                                        .estimatedDuration(act.getEstimatedDuration())
                                        .estimatedCost(act.getEstimatedCost())
                                        .orderIndex(act.getOrderIndex())
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .sorted((a, b) -> a.getDayNumber().compareTo(b.getDayNumber()))
                .collect(Collectors.toList());

        return TripResponse.builder()
                .id(trip.getId())
                .destination(trip.getDestination())
                .numberOfDays(trip.getNumberOfDays())
                .budgetType(trip.getBudgetType())
                // Copy into a plain ArrayList (not the lazy Hibernate proxy) — with
                // spring.jpa.open-in-view disabled, the session is closed by the time
                // Jackson serializes the response, so the lazy collection must be
                // materialized here, inside the active transaction.
                .interests(new ArrayList<>(trip.getInterests()))
                .itinerary(dayResponses)
                .estimatedFlightCost(trip.getEstimatedFlightCost())
                .estimatedAccommodationCost(trip.getEstimatedAccommodationCost())
                .estimatedFoodCost(trip.getEstimatedFoodCost())
                .estimatedActivitiesCost(trip.getEstimatedActivitiesCost())
                .totalEstimatedBudget(trip.getTotalEstimatedBudget())
                .status(trip.getStatus())
                .hotelSuggestions(trip.getHotelSuggestions())
                .createdAt(trip.getCreatedAt())
                .updatedAt(trip.getUpdatedAt())
                .build();
    }

    private User loadUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
