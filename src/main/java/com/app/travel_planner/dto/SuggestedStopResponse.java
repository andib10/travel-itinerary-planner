package com.app.travel_planner.dto;

/**
 * One candidate for the "add a stop" picker — a POI not already used in the trip.
 */
public record SuggestedStopResponse(
        Long id,
        String name,
        String category,
        String description,
        Double lat,
        Double lng
) {
}
