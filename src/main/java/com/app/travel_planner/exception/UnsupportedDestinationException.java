package com.app.travel_planner.exception;

/**
 * Thrown when the requested destination's city has zero ingested POIs; fails fast before any LLM call.
 */
public class UnsupportedDestinationException extends RuntimeException {

    public UnsupportedDestinationException(String message) {
        super(message);
    }
}
