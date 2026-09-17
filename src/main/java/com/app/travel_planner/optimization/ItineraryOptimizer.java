package com.app.travel_planner.optimization;

import com.app.travel_planner.entity.PointOfInterest;
import com.app.travel_planner.entity.Stop;
import com.app.travel_planner.util.CategoryHoursProvider;
import com.app.travel_planner.util.TimeWindow;
import com.app.travel_planner.util.TravelTimeCalculator;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves timing conflicts left after {@link RouteSequencer} orders a day's stops, via
 * a single deterministic forward sweep (not a general constraint solver).
 * For each stop: fix travel-time shortfalls by pushing it later (cascades forward),
 * optionally compress an oversized idle gap by pulling it earlier, then fix
 * opening-hours violations by shifting into the nearest valid window. If none exists and
 * dropping is allowed, drop the stop rather than save an invalid time.
 * A stop matching {@code resetTimeForStopId} has its carried-over start time discarded
 * first, since it just moved to a new position.
 */
public final class ItineraryOptimizer {

    // Generous on purpose - only reclaims clearly excessive dead time, not a normal break.
    private static final int MAX_IDLE_GAP_MINUTES = 90;

    private ItineraryOptimizer() {
    }

    /**
     * Defaults to full freedom: gap compression and dropping both allowed.
     */
    public static List<String> resolve(List<Stop> resequencedStops) {
        return resolve(resequencedStops, true, true, null);
    }

    /**
     * Applies {@code allowReflow} to both gap compression and dropping.
     */
    public static List<String> resolve(List<Stop> resequencedStops, boolean allowReflow) {
        return resolve(resequencedStops, allowReflow, allowReflow, null);
    }

    /**
     * Same as above, with a reset target — see {@link #resolve(List, boolean, boolean, Long)}.
     */
    public static List<String> resolve(List<Stop> resequencedStops, boolean allowReflow, Long resetTimeForStopId) {
        return resolve(resequencedStops, allowReflow, allowReflow, resetTimeForStopId);
    }

    /**
     * Mutates {@code resequencedStops}' times in place to resolve conflicts, dropping a
     * stop only when {@code allowDrop} and no valid slot exists for it.
     *
     * @param allowGapCompression whether an oversized idle gap may be pulled earlier
     * @param allowDrop           whether an unresolvable stop may be dropped instead of left invalid
     * @param resetTimeForStopId  id of a stop whose stale carried-over time should be recomputed fresh, or null
     * @return a human-readable line per fix, drop, or unresolved conflict
     */
    public static List<String> resolve(List<Stop> resequencedStops, boolean allowGapCompression, boolean allowDrop,
                                        Long resetTimeForStopId) {
        List<String> details = new ArrayList<>();
        List<Stop> toRemove = new ArrayList<>();
        Stop lastKept = null; // last stop actually kept

        for (Stop stop : resequencedStops) {
            if (stop.getStartTime() == null || stop.getEndTime() == null) {
                continue;
            }

            LocalTime earliestPermissibleStart = LocalTime.MIN;
            if (lastKept != null) {
                if (resetTimeForStopId != null && resetTimeForStopId.equals(stop.getId())) {
                    resetStaleTime(stop, computeEarliestPermissibleStart(lastKept, stop), details);
                }
                earliestPermissibleStart = resolveTravelTimeConflict(lastKept, stop, details);
                if (allowGapCompression) {
                    compressExcessiveIdleGap(stop, earliestPermissibleStart, details);
                }
            }

            boolean resolved = resolveOpeningHoursConflict(stop, earliestPermissibleStart, details);

            // Never drop the day's last remaining stop.
            boolean droppable = allowDrop && (resequencedStops.size() - toRemove.size()) > 1;
            if (!resolved && droppable) {
                toRemove.add(stop);
                details.add("Dropped: '%s' (%s-%s) removed - no valid slot exists within its schedule given the day's other stops"
                        .formatted(poiName(stop), stop.getStartTime(), stop.getEndTime()));
                continue; // not kept
            }

            lastKept = stop;
        }

        if (!toRemove.isEmpty()) {
            resequencedStops.removeAll(toRemove);
            for (int i = 0; i < resequencedStops.size(); i++) {
                resequencedStops.get(i).setOrderIndex(i + 1);
            }
        }

        return details;
    }

    /**
     * If the idle gap before {@code stop} exceeds {@link #MAX_IDLE_GAP_MINUTES}, pulls it
     * earlier to shrink the gap down to that cap (never all the way to zero slack).
     */
    private static void compressExcessiveIdleGap(Stop stop, LocalTime earliestPermissibleStart, List<String> details) {
        long gapMinutes = Duration.between(earliestPermissibleStart, stop.getStartTime()).toMinutes();
        if (gapMinutes <= MAX_IDLE_GAP_MINUTES) {
            return;
        }

        long compressionMinutes = gapMinutes - MAX_IDLE_GAP_MINUTES;
        LocalTime oldStart = stop.getStartTime();
        LocalTime oldEnd = stop.getEndTime();
        stop.setStartTime(stop.getStartTime().minusMinutes(compressionMinutes));
        stop.setEndTime(stop.getEndTime().minusMinutes(compressionMinutes));

        details.add("Idle gap: pulled '%s' earlier from %s-%s to %s-%s (-%d min) to close an oversized %d-minute gap after the previous stop"
                .formatted(poiName(stop), oldStart, oldEnd, stop.getStartTime(), stop.getEndTime(),
                        compressionMinutes, gapMinutes));
    }

    /**
     * Overwrites {@code stop}'s stale time with a fresh guess right after {@code previous}
     * ends, duration preserved; later steps may then shift it further as usual.
     */
    private static void resetStaleTime(Stop stop, LocalTime freshStart, List<String> details) {
        int durationMinutes = (int) Duration.between(stop.getStartTime(), stop.getEndTime()).toMinutes();
        LocalTime oldStart = stop.getStartTime();
        LocalTime oldEnd = stop.getEndTime();
        stop.setStartTime(freshStart);
        stop.setEndTime(freshStart.plusMinutes(durationMinutes));

        details.add("Reset: '%s' was moved to a new position, so its old time (%s-%s) is no longer meaningful - recomputed to %s-%s"
                .formatted(poiName(stop), oldStart, oldEnd, stop.getStartTime(), stop.getEndTime()));
    }

    /**
     * @return earliest start time {@code stop} may have given travel time from {@code previous}
     */
    private static LocalTime computeEarliestPermissibleStart(Stop previous, Stop stop) {
        int travelMinutes = travelMinutesBetween(previous.getPointOfInterest(), stop.getPointOfInterest());
        return previous.getEndTime().plusMinutes(travelMinutes);
    }

    private static LocalTime resolveTravelTimeConflict(Stop previous, Stop stop, List<String> details) {
        LocalTime earliestStart = computeEarliestPermissibleStart(previous, stop);

        if (stop.getStartTime().isBefore(earliestStart)) {
            int shortfallMinutes = (int) Duration.between(stop.getStartTime(), earliestStart).toMinutes();
            LocalTime oldStart = stop.getStartTime();
            LocalTime oldEnd = stop.getEndTime();
            stop.setStartTime(stop.getStartTime().plusMinutes(shortfallMinutes));
            stop.setEndTime(stop.getEndTime().plusMinutes(shortfallMinutes));

            int travelMinutes = travelMinutesBetween(previous.getPointOfInterest(), stop.getPointOfInterest());
            details.add("Travel time: shifted '%s' from %s-%s to %s-%s (+%d min) to allow %d min travel from '%s'"
                    .formatted(poiName(stop), oldStart, oldEnd, stop.getStartTime(), stop.getEndTime(),
                            shortfallMinutes, travelMinutes, poiName(previous)));
        }

        return earliestStart;
    }

    /**
     * @return true if stop was already, or was shifted to be, within its category's hours
     */
    private static boolean resolveOpeningHoursConflict(Stop stop, LocalTime earliestPermissibleStart, List<String> details) {
        String category = stop.getPointOfInterest() != null ? stop.getPointOfInterest().getCategory() : null;
        TimeWindow window = CategoryHoursProvider.windowFor(category);

        if (window.contains(stop.getStartTime()) && window.contains(stop.getEndTime())) {
            return true;
        }

        int durationMinutes = (int) Duration.between(stop.getStartTime(), stop.getEndTime()).toMinutes();
        LocalTime originalStart = stop.getStartTime();
        LocalTime originalEnd = stop.getEndTime();

        // Two candidates: earliest possible (at opening or travel floor) and latest (at closing).
        LocalTime candidateEarlyStart = laterOf(window.open(), earliestPermissibleStart);
        LocalTime candidateLateStart = window.close().minusMinutes(durationMinutes);

        LocalTime chosenStart = null;
        if (isValidCandidate(candidateEarlyStart, durationMinutes, window, earliestPermissibleStart)) {
            chosenStart = candidateEarlyStart;
        }
        if (isValidCandidate(candidateLateStart, durationMinutes, window, earliestPermissibleStart)
                && (chosenStart == null || isCloser(candidateLateStart, originalStart, chosenStart))) {
            chosenStart = candidateLateStart;
        }

        if (chosenStart == null) {
            details.add("Opening hours: '%s' scheduled %s-%s falls outside its '%s' default hours (%s-%s) — no valid slot fits the day's schedule, left unresolved"
                    .formatted(poiName(stop), originalStart, originalEnd, category, window.open(), window.close()));
            return false;
        }

        stop.setStartTime(chosenStart);
        stop.setEndTime(chosenStart.plusMinutes(durationMinutes));
        details.add("Opening hours: shifted '%s' from %s-%s to %s-%s to fit its '%s' default hours (%s-%s)"
                .formatted(poiName(stop), originalStart, originalEnd, stop.getStartTime(), stop.getEndTime(),
                        category, window.open(), window.close()));
        return true;
    }

    private static boolean isValidCandidate(LocalTime candidateStart, int durationMinutes, TimeWindow window,
                                             LocalTime earliestPermissibleStart) {
        LocalTime candidateEnd = candidateStart.plusMinutes(durationMinutes);
        return !candidateStart.isBefore(earliestPermissibleStart)
                && window.contains(candidateStart)
                && window.contains(candidateEnd);
    }

    private static boolean isCloser(LocalTime candidate, LocalTime original, LocalTime currentBest) {
        long candidateDiff = Math.abs(Duration.between(original, candidate).toMinutes());
        long currentBestDiff = Math.abs(Duration.between(original, currentBest).toMinutes());
        return candidateDiff < currentBestDiff;
    }

    private static LocalTime laterOf(LocalTime a, LocalTime b) {
        return a.isAfter(b) ? a : b;
    }

    private static int travelMinutesBetween(PointOfInterest from, PointOfInterest to) {
        return TravelTimeCalculator.travelMinutes(from.getLat(), from.getLng(), to.getLat(), to.getLng());
    }

    private static String poiName(Stop stop) {
        return stop.getPointOfInterest() != null ? stop.getPointOfInterest().getName() : "unknown";
    }
}
