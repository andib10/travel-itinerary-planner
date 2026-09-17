package com.app.travel_planner.optimization;

import com.app.travel_planner.entity.PointOfInterest;
import com.app.travel_planner.entity.Stop;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the full pass (resequence + resolve) via {@link DayOptimizer} and checks the
 * {@link OptimizationResult} counts and {@code stopsReordered} flag.
 * <p>
 * Also covers {@link DayOptimizer#optimizeDayTimingOnly}, which must fix conflicts
 * without reordering (unlike the full {@code optimizeDay}).
 */
class DayOptimizerTest {

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
    void brokenDay_reportsCorrectBeforeAfterCountsAndReorderFlag() {
        // Poor order (start -> far -> close) plus an opening-hours conflict on "close".
        PointOfInterest start = poi("Hotel-adjacent Square", "historic", 45.0, 10.0);
        PointOfInterest close = poi("Nearby Museum", "museums", 45.0 + 0.0009, 10.0); // ~100m from start
        PointOfInterest far = poi("Distant Ruins", "historic", 45.0 + FIVE_KM_IN_DEGREES_LAT, 10.0); // ~5km from start

        Stop stopStart = stop(start, LocalTime.of(9, 0), LocalTime.of(9, 30));
        Stop stopFar = stop(far, LocalTime.of(9, 35), LocalTime.of(10, 0));
        Stop stopClose = stop(close, LocalTime.of(19, 0), LocalTime.of(19, 30));

        List<Stop> day = List.of(stopStart, stopFar, stopClose);

        OptimizationResult result = DayOptimizer.optimizeDay(day);

        // 1 travel-time (far<->close after resequencing) + 1 opening-hours (close at 19:00).
        assertThat(result.conflictsBefore()).isEqualTo(2);
        assertThat(result.conflictsAfter()).isEqualTo(0);
        assertThat(result.resolvedCount()).isEqualTo(2);
        assertThat(result.unresolvedCount()).isEqualTo(0);
        assertThat(result.stopsReordered()).isTrue();
        assertThat(result.details()).isNotEmpty();
        assertThat(result.details().get(0)).contains("reordered");
    }

    @Test
    void happyDay_reportsZeroConflictsAndNoReorder() {
        PointOfInterest a = poi("Morning Ruins", "historic", 45.0, 10.0);
        PointOfInterest b = poi("Midday Museum", "museums", 45.01, 10.0);
        PointOfInterest c = poi("Afternoon Cafe", "cafes", 45.02, 10.0);

        Stop stop1 = stop(a, LocalTime.of(9, 0), LocalTime.of(10, 0));
        Stop stop2 = stop(b, LocalTime.of(10, 30), LocalTime.of(11, 30));
        Stop stop3 = stop(c, LocalTime.of(12, 0), LocalTime.of(13, 0));

        List<Stop> day = List.of(stop1, stop2, stop3);

        OptimizationResult result = DayOptimizer.optimizeDay(day);

        assertThat(result.conflictsBefore()).isZero();
        assertThat(result.conflictsAfter()).isZero();
        assertThat(result.resolvedCount()).isZero();
        assertThat(result.unresolvedCount()).isZero();
        assertThat(result.stopsReordered()).isFalse();
        assertThat(result.details()).isEmpty();
    }

    @Test
    void unresolvableDay_reportsRemainingUnresolvedConflict() {
        // Duration (12h) exceeds the category window (9h) -> never resolvable.
        PointOfInterest museum = poi("All-Day Museum Visit", "museums", 45.0, 10.0);
        Stop stop = stop(museum, LocalTime.of(7, 0), LocalTime.of(19, 0));

        List<Stop> day = List.of(stop);

        OptimizationResult result = DayOptimizer.optimizeDay(day);

        assertThat(result.conflictsBefore()).isEqualTo(1);
        assertThat(result.conflictsAfter()).isEqualTo(1);
        assertThat(result.resolvedCount()).isZero();
        assertThat(result.unresolvedCount()).isEqualTo(1);
        assertThat(result.stopsReordered()).isFalse();
        assertThat(result.details()).anyMatch(d -> d.contains("unresolved"));
    }

    @Test
    void timingOnly_fixesConflictWithoutTouchingOrder() {
        // A manually-dragged order (start -> close -> far) creates a travel-time conflict.
        // optimizeDayTimingOnly() must keep this order and shift a time, not resequence.
        PointOfInterest start = poi("Hotel-adjacent Square", "historic", 45.0, 10.0);
        PointOfInterest close = poi("Nearby Museum", "museums", 45.0 + 0.0009, 10.0); // ~100m from start
        PointOfInterest far = poi("Distant Ruins", "historic", 45.0 + FIVE_KM_IN_DEGREES_LAT, 10.0); // ~5km from start

        Stop stopStart = stop(start, LocalTime.of(9, 0), LocalTime.of(9, 30));
        Stop stopClose = stop(close, LocalTime.of(14, 0), LocalTime.of(14, 30));
        Stop stopFar = stop(far, LocalTime.of(10, 0), LocalTime.of(10, 30)); // manually dragged here

        List<Stop> day = List.of(stopStart, stopClose, stopFar);

        OptimizationResult result = DayOptimizer.optimizeDayTimingOnly(day);

        assertThat(result.stopsReordered()).isFalse();
        assertThat(result.details()).noneMatch(d -> d.contains("reordered"));
        assertThat(day).containsExactly(stopStart, stopClose, stopFar);
        // Resolved by shifting stopFar's time forward, not by reordering.
        assertThat(result.conflictsBefore()).isEqualTo(1);
        assertThat(result.conflictsAfter()).isZero();
        assertThat(result.resolvedCount()).isEqualTo(1);
        assertThat(stopFar.getStartTime()).isEqualTo(LocalTime.of(15, 29));
        assertThat(stopFar.getEndTime()).isEqualTo(LocalTime.of(15, 59));
    }

    @Test
    void optimizeDay_dropsUnresolvableStop_andSyncsBackIntoCallersOwnListReference() {
        // A drop must reach the caller's own list, not just an internal copy.
        PointOfInterest historicPoi = poi("Venue", "historic", 45.0, 10.0);
        PointOfInterest museum = poi("Museum", "museums", 45.0, 10.0); // window 09:00-18:00

        Stop first = stop(historicPoi, LocalTime.of(9, 0), LocalTime.of(10, 0));
        Stop unresolvable = stop(museum, LocalTime.of(10, 0), LocalTime.of(22, 0)); // 12h vs. 9h window
        Stop third = stop(historicPoi, LocalTime.of(10, 30), LocalTime.of(11, 0));
        List<Stop> day = new ArrayList<>(List.of(first, unresolvable, third));

        OptimizationResult result = DayOptimizer.optimizeDay(day);

        assertThat(day).containsExactly(first, third);
        assertThat(result.details()).anyMatch(d -> d.contains("Dropped"));
        assertThat(result.conflictsAfter()).isZero();
        // conflictsBefore is 2: also a travel-time conflict between unresolvable's raw
        // end and third's start, which disappears once the stop is dropped (counts as
        // resolved, not dropped, since third itself was never touched).
        assertThat(result.droppedCount()).isEqualTo(1);
        assertThat(result.resolvedCount()).isEqualTo(1);
    }

    @Test
    void resolvedCount_and_droppedCount_areTrackedSeparately_whenBothOccurInTheSameDay() {
        // Same coordinates throughout (zero travel time): one stop is rescheduled,
        // another is dropped, in the same call.
        PointOfInterest historicPoi = poi("Venue", "historic", 45.0, 10.0); // window 08:00-20:00
        PointOfInterest museum = poi("Museum", "museums", 45.0, 10.0); // window 09:00-18:00

        Stop first = stop(historicPoi, LocalTime.of(9, 0), LocalTime.of(10, 0));
        // Starts before "first" ends -> travel-time shortfall, resolved by forward push.
        Stop reschedulable = stop(historicPoi, LocalTime.of(9, 30), LocalTime.of(10, 0));
        // 12h duration vs. 9h window -> unresolvable regardless of position.
        Stop unresolvable = stop(museum, LocalTime.of(11, 0), LocalTime.of(23, 0));
        List<Stop> day = new ArrayList<>(List.of(first, reschedulable, unresolvable));

        OptimizationResult result = DayOptimizer.optimizeDay(day);

        assertThat(day).containsExactly(first, reschedulable); // unresolvable one is gone
        assertThat(reschedulable.getStartTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(reschedulable.getEndTime()).isEqualTo(LocalTime.of(10, 30));
        assertThat(result.conflictsBefore()).isEqualTo(2); // shortfall + opening-hours violation
        assertThat(result.conflictsAfter()).isZero();
        assertThat(result.resolvedCount()).isEqualTo(1);
        assertThat(result.droppedCount()).isEqualTo(1);
        assertThat(result.unresolvedCount()).isZero();
        // The three outcomes account for every original conflict, nothing double-counted.
        assertThat(result.resolvedCount() + result.droppedCount() + result.unresolvedCount())
                .isEqualTo(result.conflictsBefore());
    }
}
