package com.app.travel_planner.repository;

import com.app.travel_planner.entity.Stop;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StopRepository extends JpaRepository<Stop, Long> {

    // Blocks deleting a POI still referenced by a saved itinerary.
    long countByPointOfInterestId(Long pointOfInterestId);
}
