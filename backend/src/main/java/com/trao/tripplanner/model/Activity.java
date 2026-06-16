package com.trao.tripplanner.model;

import jakarta.persistence.*;
import lombok.*;

// SOLID-SRP: Represents a single activity within a day's itinerary
@Entity
@Table(name = "activities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_itinerary_id", nullable = false)
    private DayItinerary dayItinerary;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String timeOfDay;

    @Column(length = 200)
    private String location;

    @Column(length = 100)
    private String estimatedDuration;

    @Column(length = 100)
    private String estimatedCost;

    @Column(nullable = false)
    @Builder.Default
    private Integer orderIndex = 0;
}
