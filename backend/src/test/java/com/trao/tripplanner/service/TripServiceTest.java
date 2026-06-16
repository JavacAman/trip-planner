package com.trao.tripplanner.service;

import com.trao.tripplanner.dto.request.TripRequest;
import com.trao.tripplanner.dto.response.TripResponse;
import com.trao.tripplanner.exception.TripNotFoundException;
import com.trao.tripplanner.model.DayItinerary;
import com.trao.tripplanner.model.Trip;
import com.trao.tripplanner.model.Trip.BudgetType;
import com.trao.tripplanner.model.User;
import com.trao.tripplanner.repository.DayItineraryRepository;
import com.trao.tripplanner.repository.TripRepository;
import com.trao.tripplanner.repository.UserRepository;
import com.trao.tripplanner.service.strategy.ItineraryGenerationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TripService Unit Tests")
class TripServiceTest {

    @Mock private TripRepository tripRepository;
    @Mock private DayItineraryRepository dayItineraryRepository;
    @Mock private UserRepository userRepository;
    @Mock private GrokAiService grokAiService;
    @Mock private ItineraryGenerationStrategy itineraryGenerationStrategy;
    @Mock private ItineraryParserService parserService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TripService tripService;

    private User testUser;
    private Trip testTrip;
    private TripRequest tripRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("Test User")
                .build();

        testTrip = Trip.builder()
                .id(1L)
                .user(testUser)
                .destination("Tokyo, Japan")
                .numberOfDays(3)
                .budgetType(BudgetType.MEDIUM)
                .interests(List.of("Culture", "Food"))
                .itinerary(new ArrayList<>())
                .status(Trip.TripStatus.GENERATING)
                .build();

        tripRequest = new TripRequest();
        tripRequest.setDestination("Tokyo, Japan");
        tripRequest.setNumberOfDays(3);
        tripRequest.setBudgetType(BudgetType.MEDIUM);
        tripRequest.setInterests(List.of("Culture", "Food"));
    }

    @Test
    @DisplayName("createTrip: generates itinerary and returns completed trip")
    void createTrip_success() {
        String aiResponse = "{\"days\":[],\"budgetEstimate\":{\"total\":950}}";

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> {
            Trip t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });
        when(itineraryGenerationStrategy.buildItineraryPrompt(any())).thenReturn("prompt");
        when(itineraryGenerationStrategy.buildHotelSuggestionsPrompt(any())).thenReturn("hotel_prompt");
        when(grokAiService.generateContent(anyString())).thenReturn(aiResponse);
        when(parserService.parseItinerary(anyString(), any())).thenReturn(new ArrayList<>());
        doNothing().when(parserService).parseBudgetEstimate(anyString(), any());
        doNothing().when(eventPublisher).publishEvent(any());

        TripResponse response = tripService.createTrip(tripRequest, "user@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getDestination()).isEqualTo("Tokyo, Japan");
        assertThat(response.getStatus()).isEqualTo(Trip.TripStatus.COMPLETED);

        verify(tripRepository, times(2)).save(any(Trip.class));
        verify(grokAiService, times(2)).generateContent(anyString());
    }

    @Test
    @DisplayName("getTripById: returns trip when user owns it")
    void getTripById_success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(tripRepository.findByIdAndUserId(eq(1L), eq(1L))).thenReturn(Optional.of(testTrip));

        TripResponse response = tripService.getTripById(1L, "user@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getDestination()).isEqualTo("Tokyo, Japan");
    }

    @Test
    @DisplayName("getTripById: throws TripNotFoundException for non-existent or other-user's trip")
    void getTripById_notFound_throwsException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(tripRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.getTripById(99L, "user@example.com"))
                .isInstanceOf(TripNotFoundException.class);
    }

    @Test
    @DisplayName("getUserTrips: returns paginated trips for the authenticated user")
    void getUserTrips_success() {
        Page<Trip> page = new PageImpl<>(List.of(testTrip));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(tripRepository.findByUserId(eq(1L), any(Pageable.class))).thenReturn(page);

        Page<TripResponse> result = tripService.getUserTrips("user@example.com", Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDestination()).isEqualTo("Tokyo, Japan");
    }

    @Test
    @DisplayName("deleteTrip: deletes trip owned by the user")
    void deleteTrip_success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(tripRepository.existsByIdAndUserId(1L, 1L)).thenReturn(true);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(testTrip));
        doNothing().when(eventPublisher).publishEvent(any());

        tripService.deleteTrip(1L, "user@example.com");

        verify(tripRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteTrip: throws TripNotFoundException for other user's trip")
    void deleteTrip_unauthorized_throwsException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(tripRepository.existsByIdAndUserId(anyLong(), anyLong())).thenReturn(false);

        assertThatThrownBy(() -> tripService.deleteTrip(99L, "user@example.com"))
                .isInstanceOf(TripNotFoundException.class);

        verify(tripRepository, never()).deleteById(any());
    }
}
