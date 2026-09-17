package com.app.travel_planner.optimization;

import java.util.List;

/**
 * Summary of one {@link DayOptimizer} run on a single day's stops, for before/after metrics.
 *
 * @param conflictsBefore total conflicts before resequencing/resolving
 * @param conflictsAfter  conflicts remaining after both have run
 * @param resolvedCount   conflicts fixed in place (excludes drops); max(0, before - after - dropped)
 * @param droppedCount    stops removed because no valid slot existed; 0 when dropping disallowed
 * @param unresolvedCount conflicts still standing afterward (equals conflictsAfter)
 * @param stopsReordered  whether nearest-neighbor resequencing changed the stop order
 * @param details         human-readable line per action taken, in order
 */
public record OptimizationResult(
        int conflictsBefore,
        int conflictsAfter,
        int resolvedCount,
        int droppedCount,
        int unresolvedCount,
        boolean stopsReordered,
        List<String> details
) {
}
