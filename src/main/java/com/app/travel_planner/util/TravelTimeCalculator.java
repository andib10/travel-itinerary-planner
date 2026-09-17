package com.app.travel_planner.util;

/**
 * Converts geographic distance into estimated walking travel time, assuming a flat
 * 5 km/h walking pace instead of a real routing engine (out of scope for this project).
 */
public final class TravelTimeCalculator {

    private static final double WALKING_SPEED_KMH = 5.0;

    private TravelTimeCalculator() {
    }

    /**
     * Estimated walking time in whole minutes (rounded up) between two coordinates.
     */
    public static int travelMinutes(double lat1, double lng1, double lat2, double lng2) {
        double distanceKm = GeoUtils.haversineMeters(lat1, lng1, lat2, lng2) / 1000.0;
        return (int) Math.ceil((distanceKm / WALKING_SPEED_KMH) * 60);
    }
}
