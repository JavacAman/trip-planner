package com.trao.tripplanner.service.observer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

// Pattern-Observer: Concrete observer that reacts to trip lifecycle events
// SOLID-OCP: New reactions to events (e.g., email notifications, analytics) can be added here without touching publishers
@Component
@Slf4j
public class TripEventListener {

    // @Async ensures event handling doesn't block the main request thread
    @Async
    @EventListener
    public void handleTripCreated(TripEvent event) {
        if (event.getEventType() == TripEvent.TripEventType.TRIP_CREATED) {
            log.info("[TripEvent] Trip created — id={}, destination={}, userId={}",
                    event.getTrip().getId(),
                    event.getTrip().getDestination(),
                    event.getTrip().getUser().getId());
        }
    }

    @Async
    @EventListener
    public void handleItineraryGenerated(TripEvent event) {
        if (event.getEventType() == TripEvent.TripEventType.ITINERARY_GENERATED) {
            log.info("[TripEvent] Itinerary generated — tripId={}, days={}",
                    event.getTrip().getId(),
                    event.getTrip().getNumberOfDays());
        }
    }

    @Async
    @EventListener
    public void handleItineraryModified(TripEvent event) {
        if (event.getEventType() == TripEvent.TripEventType.ITINERARY_MODIFIED ||
                event.getEventType() == TripEvent.TripEventType.DAY_REGENERATED) {
            log.info("[TripEvent] Itinerary modified — tripId={}, type={}",
                    event.getTrip().getId(), event.getEventType());
        }
    }

    @Async
    @EventListener
    public void handleTripDeleted(TripEvent event) {
        if (event.getEventType() == TripEvent.TripEventType.TRIP_DELETED) {
            log.info("[TripEvent] Trip deleted — id={}", event.getTrip().getId());
        }
    }
}
