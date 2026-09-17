package com.app.travel_planner.optimization;

import com.app.travel_planner.entity.PointOfInterest;
import com.app.travel_planner.entity.Stop;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies ConflictDetector flags expected conflicts with no false positives/negatives.
 */
class ConflictDetectorTest {

    // ~5km per degree of latitude -> ~60 min walk at 5km/h.
    private static final double FIVE_KM_IN_DEGREES_LAT = 5 * 0.008993;

    private static PointOfInterest poi(String name, String category, double lat, double lng) {
        PointOfInterest poi = new PointOfInterest();
        poi.setName(name);
        poi.setCategory(category);
        poi.setLat(lat);
        poi.setLng(lng);
        return poi;
    }

    private static Stop stop(PointOfInterest poi, LocalTime start, LocalTime end) {
        Stop stop = new Stop();
        stop.setPointOfInterest(poi);
        stop.setStartTime(start);
        stop.setEndTime(end);
        return stop;
    }

    @Test
    void cleanDay_noConflicts() {
        PointOfInterest a = poi("Museum A", "museums", 45.0, 10.0);
        // ~100m away, negligible travel time.
        PointOfInterest b = poi("Museum B", "museums", 45.0009, 10.0);

        Stop stopA = stop(a, LocalTime.of(9, 0), LocalTime.of(10, 0));
        Stop stopB = stop(b, LocalTime.of(10, 15), LocalTime.of(11, 0));

        List<Conflict> conflicts = ConflictDetector.detect(List.of(stopA, stopB));

        assertThat(conflicts).isEmpty();
    }

    @Test
    void travelTimeConflict_detected_withCorrectShortfall() {
        PointOfInterest a = poi("Landmark A", "historic", 45.0, 10.0);
        // ~60 min walk away, only 5 min scheduled.
        PointOfInterest b = poi("Landmark B", "historic", 45.0 + FIVE_KM_IN_DEGREES_LAT, 10.0);

        Stop stopA = stop(a, LocalTime.of(10, 0), LocalTime.of(10, 0));
        Stop stopB = stop(b, LocalTime.of(10, 5), LocalTime.of(11, 0));

        List<Conflict> conflicts = ConflictDetector.detect(List.of(stopA, stopB));

        assertThat(conflicts).hasSize(1);
        Conflict conflict = conflicts.get(0);
        assertThat(conflict.type()).isEqualTo(ConflictType.TRAVEL_TIME);
        assertThat(conflict.stop()).isSameAs(stopB);
        assertThat(conflict.precedingStop()).isSameAs(stopA);
        // needs ~60 min, have 5 -> short by ~55
        assertThat(conflict.shortfallMinutes()).isBetween(53, 57);
    }

    @Test
    void openingHoursConflict_detected() {
        // museums: 09:00-18:00; scheduled after close.
        PointOfInterest museum = poi("Late Museum", "museums", 45.0, 10.0);
        Stop stop = stop(museum, LocalTime.of(20, 0), LocalTime.of(20, 30));

        List<Conflict> conflicts = ConflictDetector.detect(List.of(stop));

        assertThat(conflicts).hasSize(1);
        Conflict conflict = conflicts.get(0);
        assertThat(conflict.type()).isEqualTo(ConflictType.OPENING_HOURS);
        assertThat(conflict.stop()).isSameAs(stop);
        assertThat(conflict.precedingStop()).isNull();
    }

    @Test
    void threeStopDay_detectsBothConflictTypes_noFalsePositives() {
        PointOfInterest a = poi("Historic A", "historic", 45.0, 10.0);
        // far from A -> travel-time conflict
        PointOfInterest b = poi("Historic B", "historic", 45.0 + FIVE_KM_IN_DEGREES_LAT, 10.0);
        // close to B -> no travel-time conflict
        PointOfInterest c = poi("Late Museum C", "museums", 45.0 + FIVE_KM_IN_DEGREES_LAT, 10.0009);

        Stop stopA = stop(a, LocalTime.of(9, 0), LocalTime.of(9, 30));
        Stop stopB = stop(b, LocalTime.of(9, 35), LocalTime.of(10, 0));
        // outside museum hours -> conflict
        Stop stopC = stop(c, LocalTime.of(19, 0), LocalTime.of(19, 30));

        List<Conflict> conflicts = ConflictDetector.detect(List.of(stopA, stopB, stopC));

        assertThat(conflicts).hasSize(2);
        assertThat(conflicts).anySatisfy(conflict -> {
            assertThat(conflict.type()).isEqualTo(ConflictType.TRAVEL_TIME);
            assertThat(conflict.stop()).isSameAs(stopB);
        });
        assertThat(conflicts).anySatisfy(conflict -> {
            assertThat(conflict.type()).isEqualTo(ConflictType.OPENING_HOURS);
            assertThat(conflict.stop()).isSameAs(stopC);
        });
    }

    @Test
    void travelTime_exactlyEnoughGap_isNotAConflict() {
        // Gap exactly equals required travel time -> must not be flagged (strict ">").
        PointOfInterest a = poi("A", "historic", 45.0, 10.0);
        PointOfInterest b = poi("B", "historic", 45.0009, 10.0); // ~100m -> 2 min travel

        Stop stopA = stop(a, LocalTime.of(9, 0), LocalTime.of(9, 30));
        Stop stopB = stop(b, LocalTime.of(9, 32), LocalTime.of(10, 0)); // exactly 2 min later

        List<Conflict> conflicts = ConflictDetector.detect(List.of(stopA, stopB));

        assertThat(conflicts).isEmpty();
    }

    @Test
    void openingHours_exactlyAtWindowBoundaries_isNotAConflict() {
        // museums: 09:00-18:00, start at open, end at close.
        PointOfInterest museum = poi("Boundary Museum", "museums", 45.0, 10.0);
        Stop stop = stop(museum, LocalTime.of(9, 0), LocalTime.of(18, 0));

        List<Conflict> conflicts = ConflictDetector.detect(List.of(stop));

        assertThat(conflicts).isEmpty();
    }
}
