package com.trao.tripplanner.service.observer;

import com.trao.tripplanner.model.Trip;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

// Pattern-Observer: Event object published when trip state changes
// Spring's ApplicationEvent/ApplicationEventPublisher implements the Observer pattern
@Getter
public class TripEvent extends ApplicationEvent {

    private final Trip trip;
    private final TripEventType eventType;

    public TripEvent(Object source, Trip trip, TripEventType eventType) {
        super(source);
        this.trip = trip;
        this.eventType = eventType;
    }

    public enum TripEventType {
        TRIP_CREATED,
        ITINERARY_GENERATED,
        ITINERARY_MODIFIED,
        DAY_REGENERATED,
        TRIP_DELETED
    }
}
