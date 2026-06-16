package com.trao.tripplanner.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// SOLID-SRP: Trip entity handles only trip data persistence
// Pattern-Builder: Constructed via TripBuilder to handle optional fields gracefully
@Entity
@Table(name = "trips", indexes = {
    @Index(name = "idx_trip_user", columnList = "user_id"),
    @Index(name = "idx_trip_destination", columnList = "destination")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String destination;

    @Column(nullable = false)
    private Integer numberOfDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BudgetType budgetType;

    @ElementCollection
    @CollectionTable(name = "trip_interests", joinColumns = @JoinColumn(name = "trip_id"))
    @Column(name = "interest")
    @Builder.Default
    private List<String> interests = new ArrayList<>();

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("dayNumber ASC")
    @Builder.Default
    private List<DayItinerary> itinerary = new ArrayList<>();

    // Budget estimation fields (populated by AI)
    @Column(precision = 10, scale = 2)
    private BigDecimal estimatedFlightCost;

    @Column(precision = 10, scale = 2)
    private BigDecimal estimatedAccommodationCost;

    @Column(precision = 10, scale = 2)
    private BigDecimal estimatedFoodCost;

    @Column(precision = 10, scale = 2)
    private BigDecimal estimatedActivitiesCost;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalEstimatedBudget;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TripStatus status = TripStatus.GENERATING;

    @Column(columnDefinition = "TEXT")
    private String hotelSuggestions;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum BudgetType {
        LOW, MEDIUM, HIGH
    }

    public enum TripStatus {
        GENERATING, COMPLETED, FAILED
    }
}
