package com.app.travel_planner.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sanity-checks the Haversine-based travel time math against known coordinate pairs.
 */
class TravelTimeCalculatorTest {

    // 0.008993 degrees of latitude ~= 1 km on GeoUtils' sphere radius.
    private static final double ONE_KM_IN_DEGREES_LAT = 0.008993;

    @Test
    void oneKilometerApart_isRoughlyTwelveMinutesAtWalkingPace() {
        double lat1 = 45.0;
        double lng1 = 10.0;
        double lat2 = 45.0 + ONE_KM_IN_DEGREES_LAT;
        double lng2 = 10.0;

        int minutes = TravelTimeCalculator.travelMinutes(lat1, lng1, lat2, lng2);

        // ~1km at 5km/h = 12 min, with small rounding slack.
        assertThat(minutes).isBetween(11, 13);
    }

    @Test
    void twoKilometersApart_isRoughlyTwentyFourMinutes() {
        // Longitude delta at the equator instead of a latitude delta.
        double lat1 = 0.0;
        double lng1 = 0.0;
        double lat2 = 0.0;
        double lng2 = 2 * ONE_KM_IN_DEGREES_LAT;

        int minutes = TravelTimeCalculator.travelMinutes(lat1, lng1, lat2, lng2);

        assertThat(minutes).isBetween(23, 25);
    }

    @Test
    void samePoint_isZeroMinutes() {
        int minutes = TravelTimeCalculator.travelMinutes(41.8902, 12.4922, 41.8902, 12.4922);

        assertThat(minutes).isZero();
    }
}
