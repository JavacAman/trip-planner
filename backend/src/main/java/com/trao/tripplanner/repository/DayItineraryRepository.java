package com.trao.tripplanner.repository;

import com.trao.tripplanner.model.DayItinerary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DayItineraryRepository extends JpaRepository<DayItinerary, Long> {
    Optional<DayItinerary> findByTripIdAndDayNumber(Long tripId, Integer dayNumber);
}
