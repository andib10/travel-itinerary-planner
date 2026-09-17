package com.app.travel_planner.optimization;

import com.app.travel_planner.entity.PointOfInterest;
import com.app.travel_planner.entity.Stop;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies ItineraryOptimizer resolves travel-time and opening-hours conflicts where
 * possible, flags unresolvable ones, and never increases the conflict count.
 */
class ItineraryOptimizerTest {

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
    void travelTimeConflict_isResolved_gapSufficientAfterward() {
        PointOfInterest a = poi("Landmark A", "historic", 45.0, 10.0);
        PointOfInterest b = poi("Landmark B", "historic", 45.0 + FIVE_KM_IN_DEGREES_LAT, 10.0);

        Stop stopA = stop(a, LocalTime.of(10, 0), LocalTime.of(10, 0));
        Stop stopB = stop(b, LocalTime.of(10, 5), LocalTime.of(11, 0));
        List<Stop> day = new ArrayList<>(List.of(stopA, stopB));

        assertThat(ConflictDetector.detect(day)).hasSize(1);

        List<String> details = ItineraryOptimizer.resolve(day);

        assertThat(details).isNotEmpty();
        assertThat(ConflictDetector.detect(day)).isEmpty();
        // Duration preserved by the shift.
        assertThat(java.time.Duration.between(stopB.getStartTime(), stopB.getEndTime()).toMinutes()).isEqualTo(55);
    }

    @Test
    void openingHoursConflict_isResolved_whenValidSlotExists() {
        // Scheduled before opening -> should shift forward to opening time.
        PointOfInterest museum = poi("Early Museum", "museums", 45.0, 10.0);
        Stop stop = stop(museum, LocalTime.of(7, 0), LocalTime.of(7, 30));
        List<Stop> day = new ArrayList<>(List.of(stop));

        assertThat(ConflictDetector.detect(day)).hasSize(1);

        List<String> details = ItineraryOptimizer.resolve(day);

        assertThat(details).isNotEmpty();
        assertThat(ConflictDetector.detect(day)).isEmpty();
        assertThat(stop.getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(stop.getEndTime()).isEqualTo(LocalTime.of(9, 30));
    }

    @Test
    void openingHoursConflict_isFlaggedUnresolved_whenNoValidSlotFits() {
        // Duration (12h) exceeds the category window (9h) -> no slot fits.
        PointOfInterest museum = poi("All-Day Museum Visit", "museums", 45.0, 10.0);
        Stop stop = stop(museum, LocalTime.of(7, 0), LocalTime.of(19, 0));
        List<Stop> day = new ArrayList<>(List.of(stop));

        List<Conflict> before = ConflictDetector.detect(day);
        assertThat(before).hasSize(1);

        List<String> details = ItineraryOptimizer.resolve(day);

        // Stop left untouched.
        assertThat(stop.getStartTime()).isEqualTo(LocalTime.of(7, 0));
        assertThat(stop.getEndTime()).isEqualTo(LocalTime.of(19, 0));
        assertThat(details).anyMatch(d -> d.contains("unresolved"));

        List<Conflict> after = ConflictDetector.detect(day);
        assertThat(after).hasSize(1);
        assertThat(after.get(0).type()).isEqualTo(ConflictType.OPENING_HOURS);
    }

    @Test
    void brokenResequencedDay_conflictCountDecreasesAndNeverIncreases() {
        // Poor original order (fixed by RouteSequencer) plus a lingering opening-hours conflict.
        PointOfInterest start = poi("Hotel-adjacent Square", "historic", 45.0, 10.0);
        PointOfInterest close = poi("Nearby Museum", "museums", 45.0 + 0.0009, 10.0); // ~100m from start
        PointOfInterest far = poi("Distant Ruins", "historic", 45.0 + FIVE_KM_IN_DEGREES_LAT, 10.0); // ~5km from start

        Stop stopStart = stop(start, LocalTime.of(9, 0), LocalTime.of(9, 30));
        // Poor order (start -> far -> close) causes backtracking; close is also outside hours.
        Stop stopFar = stop(far, LocalTime.of(9, 35), LocalTime.of(10, 0));
        Stop stopClose = stop(close, LocalTime.of(19, 0), LocalTime.of(19, 30));

        List<Stop> original = List.of(stopStart, stopFar, stopClose);
        List<Conflict> conflictsBeforeResequence = ConflictDetector.detect(original);
        assertThat(conflictsBeforeResequence).isNotEmpty();

        List<Stop> resequenced = RouteSequencer.resequence(new ArrayList<>(original));
        // Nearest-neighbor visits the closer museum before the distant ruins.
        assertThat(resequenced).containsExactly(stopStart, stopClose, stopFar);

        List<Conflict> conflictsBefore = ConflictDetector.detect(resequenced);
        assertThat(conflictsBefore).isNotEmpty();

        List<String> details = ItineraryOptimizer.resolve(resequenced);
        assertThat(details).isNotEmpty();

        List<Conflict> conflictsAfter = ConflictDetector.detect(resequenced);

        assertThat(conflictsAfter.size()).isLessThanOrEqualTo(conflictsBefore.size());
        assertThat(conflictsAfter.size()).isLessThan(conflictsBefore.size());
        // Travel-time conflicts always resolve via a forward shift, so any remaining
        // conflict must be an opening-hours one.
        assertThat(conflictsAfter).allMatch(c -> c.type() == ConflictType.OPENING_HOURS);
        // Order untouched by the resolve step.
        assertThat(resequenced).containsExactly(stopStart, stopClose, stopFar);
    }

    @Test
    void happyPath_alreadyValidDay_isUntouchedByResequenceAndResolve() {
        // Sequential, generous gaps, within hours -> nothing needs fixing.
        PointOfInterest a = poi("Morning Ruins", "historic", 45.0, 10.0);
        PointOfInterest b = poi("Midday Museum", "museums", 45.01, 10.0); // ~1.1km from A
        PointOfInterest c = poi("Afternoon Cafe", "cafes", 45.02, 10.0); // ~1.1km from B

        Stop stop1 = stop(a, LocalTime.of(9, 0), LocalTime.of(10, 0));
        Stop stop2 = stop(b, LocalTime.of(10, 30), LocalTime.of(11, 30));
        Stop stop3 = stop(c, LocalTime.of(12, 0), LocalTime.of(13, 0));
        List<Stop> day = new ArrayList<>(List.of(stop1, stop2, stop3));

        LocalTime[] originalTimes = {
                stop1.getStartTime(), stop1.getEndTime(),
                stop2.getStartTime(), stop2.getEndTime(),
                stop3.getStartTime(), stop3.getEndTime()
        };

        assertThat(ConflictDetector.detect(day)).isEmpty();

        List<Stop> resequenced = RouteSequencer.resequence(day);
        assertThat(resequenced).containsExactly(stop1, stop2, stop3);
        assertThat(stop1.getOrderIndex()).isEqualTo(1);
        assertThat(stop2.getOrderIndex()).isEqualTo(2);
        assertThat(stop3.getOrderIndex()).isEqualTo(3);

        List<String> details = ItineraryOptimizer.resolve(resequenced);

        assertThat(details).isEmpty();
        assertThat(stop1.getStartTime()).isEqualTo(originalTimes[0]);
        assertThat(stop1.getEndTime()).isEqualTo(originalTimes[1]);
        assertThat(stop2.getStartTime()).isEqualTo(originalTimes[2]);
        assertThat(stop2.getEndTime()).isEqualTo(originalTimes[3]);
        assertThat(stop3.getStartTime()).isEqualTo(originalTimes[4]);
        assertThat(stop3.getEndTime()).isEqualTo(originalTimes[5]);
        assertThat(ConflictDetector.detect(resequenced)).isEmpty();
    }

    @Test
    void openingHoursConflict_resolved_forMidnightSpanningCategory() {
        // pubs: 17:00-01:00 (spans midnight). Scheduled in the closed daytime gap ->
        // should shift forward to 17:00, not back into the early-morning tail.
        PointOfInterest pub = poi("Corner Pub", "pubs", 45.0, 10.0);
        Stop stop = stop(pub, LocalTime.of(14, 0), LocalTime.of(15, 0));
        List<Stop> day = new ArrayList<>(List.of(stop));

        assertThat(ConflictDetector.detect(day)).hasSize(1);

        List<String> details = ItineraryOptimizer.resolve(day);

        assertThat(details).isNotEmpty();
        assertThat(stop.getStartTime()).isEqualTo(LocalTime.of(17, 0));
        assertThat(stop.getEndTime()).isEqualTo(LocalTime.of(18, 0));
        assertThat(ConflictDetector.detect(day)).isEmpty();
    }

    @Test
    void cascadingTravelTimeConflicts_acrossThreeStops_allResolved() {
        // Each pair ~60 min apart, only 5 min scheduled -> fix must cascade to stop 3.
        PointOfInterest a = poi("Stop A", "historic", 45.0, 10.0);
        PointOfInterest b = poi("Stop B", "historic", 45.0 + FIVE_KM_IN_DEGREES_LAT, 10.0);
        PointOfInterest c = poi("Stop C", "historic", 45.0 + 2 * FIVE_KM_IN_DEGREES_LAT, 10.0);

        Stop stopA = stop(a, LocalTime.of(9, 0), LocalTime.of(9, 30));
        Stop stopB = stop(b, LocalTime.of(9, 35), LocalTime.of(10, 0));
        Stop stopC = stop(c, LocalTime.of(10, 5), LocalTime.of(10, 30));
        List<Stop> day = new ArrayList<>(List.of(stopA, stopB, stopC));

        assertThat(ConflictDetector.detect(day)).hasSize(2);

        List<String> details = ItineraryOptimizer.resolve(day);

        assertThat(details).hasSize(2);
        assertThat(ConflictDetector.detect(day)).isEmpty();
        // Order and durations preserved.
        assertThat(stopA.getEndTime()).isBefore(stopB.getStartTime());
        assertThat(stopB.getEndTime()).isBefore(stopC.getStartTime());
        assertThat(java.time.Duration.between(stopB.getStartTime(), stopB.getEndTime()).toMinutes()).isEqualTo(25);
        assertThat(java.time.Duration.between(stopC.getStartTime(), stopC.getEndTime()).toMinutes()).isEqualTo(25);
    }

    @Test
    void excessiveIdleGap_isCompressed_whenAllowed() {
        // Same POI (zero travel time) isolates gap size from opening-hours effects.
        PointOfInterest a = poi("Morning Stop", "historic", 45.0, 10.0);
        Stop stopA = stop(a, LocalTime.of(9, 0), LocalTime.of(10, 0));
        // 5-hour gap; only 90 min of it is legitimate slack.
        Stop stopB = stop(a, LocalTime.of(15, 0), LocalTime.of(15, 30));
        List<Stop> day = new ArrayList<>(List.of(stopA, stopB));

        assertThat(ConflictDetector.detect(day)).isEmpty(); // a large gap alone isn't a conflict

        List<String> details = ItineraryOptimizer.resolve(day, true);

        assertThat(details).anyMatch(d -> d.contains("Idle gap"));
        // Pulled back to a 90-min gap after stopA's end; duration preserved.
        assertThat(stopB.getStartTime()).isEqualTo(LocalTime.of(11, 30));
        assertThat(stopB.getEndTime()).isEqualTo(LocalTime.of(12, 0));
    }

    @Test
    void excessiveIdleGap_isNotTouched_whenCompressionDisallowed() {
        // A gap the user left on purpose must never be silently closed.
        PointOfInterest a = poi("Morning Stop", "historic", 45.0, 10.0);
        Stop stopA = stop(a, LocalTime.of(9, 0), LocalTime.of(10, 0));
        Stop stopB = stop(a, LocalTime.of(15, 0), LocalTime.of(15, 30));
        List<Stop> day = new ArrayList<>(List.of(stopA, stopB));

        List<String> details = ItineraryOptimizer.resolve(day, false);

        assertThat(details).isEmpty();
        assertThat(stopB.getStartTime()).isEqualTo(LocalTime.of(15, 0));
        assertThat(stopB.getEndTime()).isEqualTo(LocalTime.of(15, 30));
    }

    @Test
    void excessiveGapCascade_leftUnresolved_withoutCompression_reproducingTheLiveBug() {
        // An unreclaimed idle gap lets a later stop drift past its category's close
        // time with nothing left to shift into. Same POI throughout isolates the gap
        // effect from travel-time shifting.
        PointOfInterest historicPoi = poi("Venue", "historic", 45.0, 10.0);
        PointOfInterest cultural = poi("Cultural Stop", "cultural", 45.0, 10.0); // window 09:00-19:00

        Stop morning = stop(historicPoi, LocalTime.of(9, 0), LocalTime.of(10, 15));
        // ~6h45m gap, valid within cultural's own window.
        Stop midday = stop(cultural, LocalTime.of(17, 0), LocalTime.of(19, 0));
        // Fine on travel time, but ends 30 min past historic's 20:00 close.
        Stop evening = stop(historicPoi, LocalTime.of(19, 5), LocalTime.of(20, 35));
        List<Stop> day = new ArrayList<>(List.of(morning, midday, evening));

        assertThat(ConflictDetector.detect(day)).hasSize(1);

        List<String> details = ItineraryOptimizer.resolve(day, false);

        assertThat(details).anyMatch(d -> d.contains("unresolved"));
        assertThat(ConflictDetector.detect(day)).hasSize(1); // still broken
    }

    @Test
    void excessiveGapCascade_resolved_withCompression() {
        // Same shape as above, this time with compression enabled - proves the fix.
        PointOfInterest historicPoi = poi("Venue", "historic", 45.0, 10.0);
        PointOfInterest cultural = poi("Cultural Stop", "cultural", 45.0, 10.0);

        Stop morning = stop(historicPoi, LocalTime.of(9, 0), LocalTime.of(10, 15));
        Stop midday = stop(cultural, LocalTime.of(17, 0), LocalTime.of(19, 0));
        Stop evening = stop(historicPoi, LocalTime.of(19, 5), LocalTime.of(20, 35));
        List<Stop> day = new ArrayList<>(List.of(morning, midday, evening));

        assertThat(ConflictDetector.detect(day)).hasSize(1);

        List<String> details = ItineraryOptimizer.resolve(day, true);

        assertThat(details).anyMatch(d -> d.contains("Idle gap"));
        assertThat(details).noneMatch(d -> d.contains("unresolved"));
        assertThat(ConflictDetector.detect(day)).isEmpty();
        // Both compressed to a 90-min gap after the previous stop; durations preserved.
        assertThat(midday.getStartTime()).isEqualTo(LocalTime.of(11, 45));
        assertThat(midday.getEndTime()).isEqualTo(LocalTime.of(13, 45));
        assertThat(evening.getStartTime()).isEqualTo(LocalTime.of(15, 15));
        assertThat(evening.getEndTime()).isEqualTo(LocalTime.of(16, 45));
    }

    @Test
    void unresolvableStop_isDropped_andLaterStopComparesAgainstTheLastKeptStop() {
        // An unresolvable stop must be dropped, and the next stop must compare against
        // the last *kept* stop, not the dropped one's broken time.
        PointOfInterest historicPoi = poi("Venue", "historic", 45.0, 10.0); // window 08:00-20:00
        PointOfInterest museum = poi("Museum", "museums", 45.0, 10.0); // window 09:00-18:00

        Stop first = stop(historicPoi, LocalTime.of(9, 0), LocalTime.of(10, 0));
        // 12h duration vs. a 9h window - unresolvable regardless of placement.
        Stop unresolvable = stop(museum, LocalTime.of(10, 0), LocalTime.of(22, 0));
        Stop third = stop(historicPoi, LocalTime.of(10, 30), LocalTime.of(11, 0));
        List<Stop> day = new ArrayList<>(List.of(first, unresolvable, third));

        // Also a travel-time conflict between unresolvable's raw end and third's start.
        assertThat(ConflictDetector.detect(day)).hasSize(2);

        List<String> details = ItineraryOptimizer.resolve(day, true);

        assertThat(details).anyMatch(d -> d.contains("unresolved"));
        assertThat(details).anyMatch(d -> d.contains("Dropped"));
        assertThat(day).containsExactly(first, third); // unresolvable stop is gone
        assertThat(first.getOrderIndex()).isEqualTo(1);
        assertThat(third.getOrderIndex()).isEqualTo(2); // renumbered after the drop
        // Untouched - proves third was compared against first, not the dropped stop.
        assertThat(third.getStartTime()).isEqualTo(LocalTime.of(10, 30));
        assertThat(third.getEndTime()).isEqualTo(LocalTime.of(11, 0));
        assertThat(ConflictDetector.detect(day)).isEmpty();
    }

    @Test
    void unresolvableStop_isNeverDropped_ifItWouldEmptyTheDay() {
        // A day must never end up with zero stops, even if its only stop is unresolvable.
        PointOfInterest museum = poi("All-Day Museum Visit", "museums", 45.0, 10.0);
        Stop onlyStop = stop(museum, LocalTime.of(7, 0), LocalTime.of(19, 0)); // 12h vs. a 9h window
        List<Stop> day = new ArrayList<>(List.of(onlyStop));

        List<String> details = ItineraryOptimizer.resolve(day, true);

        assertThat(day).hasSize(1).containsExactly(onlyStop);
        assertThat(details).noneMatch(d -> d.contains("Dropped"));
        assertThat(details).anyMatch(d -> d.contains("unresolved"));
    }

    @Test
    void resetTimeForStopId_recomputesTheMovedStop_insteadOfAnchoringLaterStopsAgainstItsStaleTime() {
        // A stop dragged into a new position still carries its old time (dragging only
        // changes order); resolve() must reset it instead of anchoring later stops to it.
        PointOfInterest historicPoi = poi("Landmark", "historic", 45.0, 10.0); // window 08:00-20:00
        PointOfInterest cafe = poi("Café", "restaurants", 45.0, 10.0); // window 12:00-23:00

        Stop stopA = stop(historicPoi, LocalTime.of(9, 0), LocalTime.of(11, 0));
        Stop stopB = stop(cafe, LocalTime.of(17, 0), LocalTime.of(18, 30)); // dragged here, stale time
        stopB.setId(99L); // the id the frontend would tell the backend was moved
        Stop stopC = stop(historicPoi, LocalTime.of(12, 0), LocalTime.of(13, 30));
        Stop stopD = stop(historicPoi, LocalTime.of(14, 0), LocalTime.of(15, 0));
        List<Stop> day = new ArrayList<>(List.of(stopA, stopB, stopC, stopD));

        List<String> details = ItineraryOptimizer.resolve(day, false, stopB.getId());

        assertThat(details).anyMatch(d -> d.contains("Reset"));
        assertThat(ConflictDetector.detect(day)).isEmpty();
        // Order untouched - only times changed.
        assertThat(day).containsExactly(stopA, stopB, stopC, stopD);
        assertThat(stopA.getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(stopA.getEndTime()).isEqualTo(LocalTime.of(11, 0));
        // Reset after A, nudged to 12:00 since restaurants open then - not stale 17:00.
        assertThat(stopB.getStartTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(stopB.getEndTime()).isEqualTo(LocalTime.of(13, 30));
        // C and D cascade off B's new end time.
        assertThat(stopC.getStartTime()).isEqualTo(LocalTime.of(13, 30));
        assertThat(stopC.getEndTime()).isEqualTo(LocalTime.of(15, 0));
        assertThat(stopD.getStartTime()).isEqualTo(LocalTime.of(15, 0));
        assertThat(stopD.getEndTime()).isEqualTo(LocalTime.of(16, 0));
    }

    @Test
    void withoutResetTimeForStopId_sameScenario_cascadesIntoTheEveningInstead() {
        // Same shape minus the reset - documents the "before" behavior being fixed.
        PointOfInterest historicPoi = poi("Landmark", "historic", 45.0, 10.0);
        PointOfInterest cafe = poi("Café", "restaurants", 45.0, 10.0);

        Stop stopA = stop(historicPoi, LocalTime.of(9, 0), LocalTime.of(11, 0));
        Stop stopB = stop(cafe, LocalTime.of(17, 0), LocalTime.of(18, 30));
        Stop stopC = stop(historicPoi, LocalTime.of(12, 0), LocalTime.of(13, 30));
        Stop stopD = stop(historicPoi, LocalTime.of(14, 0), LocalTime.of(15, 0));
        List<Stop> day = new ArrayList<>(List.of(stopA, stopB, stopC, stopD));

        ItineraryOptimizer.resolve(day, false); // no resetTimeForStopId at all

        assertThat(stopB.getStartTime()).isEqualTo(LocalTime.of(17, 0)); // untouched
        assertThat(stopC.getStartTime()).isAfter(LocalTime.of(18, 0)); // shoved into the evening
        assertThat(stopD.getStartTime()).isEqualTo(LocalTime.of(20, 0)); // right at historic's closing hour...
        assertThat(stopD.getEndTime()).isAfter(LocalTime.of(20, 0)); // ...so its end spills past it
        assertThat(ConflictDetector.detect(day)).isNotEmpty(); // D ends up unresolved
    }

    @Test
    void allowGapCompression_independentOfAllowDrop_closesGapsButNeverDropsAnything() {
        // allowGapCompression=true must never also authorize dropping a stop.
        PointOfInterest historicPoi = poi("Landmark", "historic", 45.0, 10.0); // window 08:00-20:00
        PointOfInterest museum = poi("Museum", "museums", 45.0, 10.0); // window 09:00-18:00

        Stop stop1 = stop(historicPoi, LocalTime.of(9, 0), LocalTime.of(10, 0));
        Stop stop2 = stop(historicPoi, LocalTime.of(15, 0), LocalTime.of(15, 30)); // big gap after stop1
        Stop stop3 = stop(museum, LocalTime.of(7, 0), LocalTime.of(19, 0)); // 12h vs. 9h window - always unresolvable
        List<Stop> day = new ArrayList<>(List.of(stop1, stop2, stop3));

        List<String> details = ItineraryOptimizer.resolve(day, true, false, null); // compression on, drop off

        assertThat(details).anyMatch(d -> d.contains("Idle gap"));
        assertThat(details).anyMatch(d -> d.contains("unresolved"));
        assertThat(details).noneMatch(d -> d.contains("Dropped"));
        assertThat(day).hasSize(3).containsExactly(stop1, stop2, stop3); // nothing removed
        // The gap after stop1 was actually closed (capped at the 90-minute limit).
        assertThat(stop2.getStartTime()).isEqualTo(LocalTime.of(11, 30));
        assertThat(stop2.getEndTime()).isEqualTo(LocalTime.of(12, 0));
    }
}
