package com.app.travel_planner.exception;

/**
 * Thrown when OpenTripMap's geocoder can't resolve the requested city name.
 */
public class CityNotFoundException extends RuntimeException {

    public CityNotFoundException(String message) {
        super(message);
    }
}
