package com.trao.tripplanner.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

// SOLID-SRP: Represents a single day's plan within a trip
@Entity
@Table(name = "day_itineraries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DayItinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false)
    private Integer dayNumber;

    @Column(length = 500)
    private String theme;

    @OneToMany(mappedBy = "dayItinerary", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<Activity> activities = new ArrayList<>();
}
