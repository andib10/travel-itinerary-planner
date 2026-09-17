package com.app.travel_planner.optimization;

import com.app.travel_planner.entity.Stop;

/**
 * A single detected scheduling conflict for one day's stops. For TRAVEL_TIME, {@code stop}
 * is the later stop, {@code precedingStop} the earlier one, and {@code shortfallMinutes}
 * the gap deficit. For OPENING_HOURS, {@code precedingStop} is null and shortfall is 0.
 */
public record Conflict(
        ConflictType type,
        Stop stop,
        Stop precedingStop,
        int shortfallMinutes,
        String description
) {
}
