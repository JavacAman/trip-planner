package com.trao.tripplanner.controller;

import com.trao.tripplanner.dto.request.ActivityRequest;
import com.trao.tripplanner.dto.request.ModifyDayRequest;
import com.trao.tripplanner.dto.request.TripRequest;
import com.trao.tripplanner.dto.response.ApiResponse;
import com.trao.tripplanner.dto.response.TripResponse;
import com.trao.tripplanner.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

// SOLID-SRP: Handles only HTTP routing and request/response translation for trips
// Authorization: All endpoints require authenticated user — ownership enforced in TripService
@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
@Tag(name = "Trips", description = "Trip planning and itinerary management")
@SecurityRequirement(name = "bearerAuth")
public class TripController {

    private final TripService tripService;

    @PostMapping
    @Operation(summary = "Create a new trip and generate AI itinerary")
    public ResponseEntity<ApiResponse<TripResponse>> createTrip(
            @Valid @RequestBody TripRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        TripResponse response = tripService.createTrip(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Trip created and itinerary generated"));
    }

    @GetMapping
    @Operation(summary = "Get all trips for the authenticated user (paginated)")
    public ResponseEntity<ApiResponse<Page<TripResponse>>> getUserTrips(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        Page<TripResponse> trips = tripService.getUserTrips(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(ApiResponse.success(trips, "Trips retrieved"));
    }

    @GetMapping("/{tripId}")
    @Operation(summary = "Get a specific trip by ID")
    public ResponseEntity<ApiResponse<TripResponse>> getTripById(
            @PathVariable Long tripId,
            @AuthenticationPrincipal UserDetails userDetails) {
        TripResponse response = tripService.getTripById(tripId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Trip retrieved"));
    }

    @PostMapping("/{tripId}/days/{dayNumber}/regenerate")
    @Operation(summary = "Regenerate a specific day with custom instructions")
    public ResponseEntity<ApiResponse<TripResponse>> regenerateDay(
            @PathVariable Long tripId,
            @PathVariable Integer dayNumber,
            @Valid @RequestBody ModifyDayRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        TripResponse response = tripService.regenerateDay(tripId, dayNumber, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Day " + dayNumber + " regenerated"));
    }

    @PostMapping("/{tripId}/activities")
    @Operation(summary = "Add a new activity to a specific day")
    public ResponseEntity<ApiResponse<TripResponse>> addActivity(
            @PathVariable Long tripId,
            @Valid @RequestBody ActivityRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        TripResponse response = tripService.addActivity(tripId, request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Activity added"));
    }

    @DeleteMapping("/{tripId}/activities/{activityId}")
    @Operation(summary = "Remove an activity from a trip")
    public ResponseEntity<ApiResponse<TripResponse>> removeActivity(
            @PathVariable Long tripId,
            @PathVariable Long activityId,
            @AuthenticationPrincipal UserDetails userDetails) {
        TripResponse response = tripService.removeActivity(tripId, activityId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Activity removed"));
    }

    @DeleteMapping("/{tripId}")
    @Operation(summary = "Delete a trip")
    public ResponseEntity<ApiResponse<Void>> deleteTrip(
            @PathVariable Long tripId,
            @AuthenticationPrincipal UserDetails userDetails) {
        tripService.deleteTrip(tripId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null, "Trip deleted"));
    }
}
