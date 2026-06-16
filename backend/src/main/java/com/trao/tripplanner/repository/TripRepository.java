package com.trao.tripplanner.repository;

import com.trao.tripplanner.model.Trip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// SOLID-ISP: Repository exposes only trip-specific queries
@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    // Data isolation: always scope by userId to prevent cross-user access
    Page<Trip> findByUserId(Long userId, Pageable pageable);

    // Security: verify ownership before returning trip
    Optional<Trip> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COUNT(t) FROM Trip t WHERE t.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);
}
