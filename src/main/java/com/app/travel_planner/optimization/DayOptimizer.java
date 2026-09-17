package com.app.travel_planner.optimization;

import com.app.travel_planner.entity.Stop;

import java.util.ArrayList;
import java.util.List;

/**
 * Two entry points reporting via {@link OptimizationResult}: {@link #optimizeDay} does a
 * full pass (resequence + timing resolution) after generation; {@link #optimizeDayTimingOnly}
 * only resolves timing, preserving a user's manual order and gaps.
 */
public final class DayOptimizer {

    private DayOptimizer() {
    }

    /**
     * @param currentOrderStops mutated in place: orderIndex and start/end times
     */
    public static OptimizationResult optimizeDay(List<Stop> currentOrderStops) {
        return run(currentOrderStops, true, null, true);
    }

    /**
     * @param currentOrderStops order treated as fixed; only start/end times are mutated
     */
    public static OptimizationResult optimizeDayTimingOnly(List<Stop> currentOrderStops) {
        return run(currentOrderStops, false, null, false);
    }

    /**
     * Same as {@link #optimizeDayTimingOnly(List)}, but recomputes one stop's stale time
     * from scratch (e.g. after being dragged to a new position).
     *
     * @param resetTimeForStopId the moved stop's id, or null for default behavior
     */
    public static OptimizationResult optimizeDayTimingOnly(List<Stop> currentOrderStops, Long resetTimeForStopId) {
        return run(currentOrderStops, false, resetTimeForStopId, false);
    }

    /**
     * Same as {@link #optimizeDayTimingOnly(List, Long)}, but also allows closing an
     * oversized idle gap (opt-in; does not enable dropping a stop).
     *
     * @param allowGapCompression opt-in for this call only; resequencing/dropping stay off
     */
    public static OptimizationResult optimizeDayTimingOnly(List<Stop> currentOrderStops, Long resetTimeForStopId,
                                                             boolean allowGapCompression) {
        return run(currentOrderStops, false, resetTimeForStopId, allowGapCompression);
    }

    private static OptimizationResult run(List<Stop> currentOrderStops, boolean allowResequence, Long resetTimeForStopId,
                                           boolean allowGapCompression) {
        int conflictsBefore = ConflictDetector.detect(currentOrderStops).size();

        List<Stop> sequenced = currentOrderStops;
        boolean stopsReordered = false;
        if (allowResequence) {
            List<Stop> originalOrder = new ArrayList<>(currentOrderStops);
            sequenced = RouteSequencer.resequence(currentOrderStops);
            stopsReordered = !originalOrder.equals(sequenced);
        }

        List<String> details = new ArrayList<>();
        if (stopsReordered) {
            details.add("Stops reordered geographically (nearest-neighbor) for a shorter route.");
        }
        // allowResequence also gates dropping - both only happen on the full generation pass.
        int stopCountBeforeResolve = sequenced.size();
        details.addAll(ItineraryOptimizer.resolve(sequenced, allowGapCompression, allowResequence, resetTimeForStopId));
        // resolve() removes dropped stops before returning, so the size delta is the drop count.
        int droppedCount = stopCountBeforeResolve - sequenced.size();

        // Only sync back on a size mismatch; list order doesn't matter since callers re-sort by orderIndex.
        if (sequenced.size() != currentOrderStops.size()) {
            currentOrderStops.clear();
            currentOrderStops.addAll(sequenced);
        }

        int conflictsAfter = ConflictDetector.detect(sequenced).size();
        // Exclude droppedCount: a dropped stop's conflict disappearing isn't the same as being resolved.
        int resolvedCount = Math.max(0, conflictsBefore - conflictsAfter - droppedCount);
        int unresolvedCount = conflictsAfter;

        return new OptimizationResult(conflictsBefore, conflictsAfter, resolvedCount, droppedCount, unresolvedCount,
                stopsReordered, details);
    }
}
