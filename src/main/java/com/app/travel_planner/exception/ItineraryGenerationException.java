package com.app.travel_planner.exception;

import java.util.List;

/**
 * Thrown when the LLM's itinerary JSON still fails validation after one repair
 * retry — the request fails cleanly rather than saving broken data.
 */
public class ItineraryGenerationException extends RuntimeException {

    private final List<String> errors;

    public ItineraryGenerationException(List<String> errors) {
        super("Itinerary generation failed validation: " + errors);
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
