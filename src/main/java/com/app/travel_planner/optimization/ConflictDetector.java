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
 * Detects (but does not fix) scheduling conflicts: insufficient travel time between
 * consecutive stops, and stops scheduled outside their category's opening hours.
 * Pure/stateless — reused both before and after resolution for before/after metrics.
 */
public final class ConflictDetector {

    private ConflictDetector() {
    }

    /**
     * @param orderedStops stops in sequence (order matters for travel-time checks only)
     */
    public static List<Conflict> detect(List<Stop> orderedStops) {
        List<Conflict> conflicts = new ArrayList<>();
        conflicts.addAll(detectTravelTimeConflicts(orderedStops));
        conflicts.addAll(detectOpeningHoursConflicts(orderedStops));
        return conflicts;
    }

    private static List<Conflict> detectTravelTimeConflicts(List<Stop> orderedStops) {
        List<Conflict> conflicts = new ArrayList<>();
        for (int i = 0; i < orderedStops.size() - 1; i++) {
            Stop from = orderedStops.get(i);
            Stop to = orderedStops.get(i + 1);
            if (from.getEndTime() == null || to.getStartTime() == null) {
                continue;
            }

            int travelMinutes = travelMinutesBetween(from.getPointOfInterest(), to.getPointOfInterest());
            LocalTime earliestArrival = from.getEndTime().plusMinutes(travelMinutes);

            if (earliestArrival.isAfter(to.getStartTime())) {
                int shortfallMinutes = (int) Duration.between(to.getStartTime(), earliestArrival).toMinutes();
                String description = "Only %d min between '%s' (ends %s) and '%s' (starts %s), but travel takes %d min — short by %d min"
                        .formatted(Duration.between(from.getEndTime(), to.getStartTime()).toMinutes(),
                                poiName(from), from.getEndTime(), poiName(to), to.getStartTime(),
                                travelMinutes, shortfallMinutes);
                conflicts.add(new Conflict(ConflictType.TRAVEL_TIME, to, from, shortfallMinutes, description));
            }
        }
        return conflicts;
    }

    private static List<Conflict> detectOpeningHoursConflicts(List<Stop> orderedStops) {
        List<Conflict> conflicts = new ArrayList<>();
        for (Stop stop : orderedStops) {
            if (stop.getStartTime() == null || stop.getEndTime() == null) {
                continue;
            }

            String category = stop.getPointOfInterest() != null ? stop.getPointOfInterest().getCategory() : null;
            TimeWindow window = CategoryHoursProvider.windowFor(category);

            boolean startWithinHours = window.contains(stop.getStartTime());
            boolean endWithinHours = window.contains(stop.getEndTime());

            if (!startWithinHours || !endWithinHours) {
                String description = "'%s' scheduled %s-%s falls outside its '%s' default hours (%s-%s)"
                        .formatted(poiName(stop), stop.getStartTime(), stop.getEndTime(),
                                category, window.open(), window.close());
                conflicts.add(new Conflict(ConflictType.OPENING_HOURS, stop, null, 0, description));
            }
        }
        return conflicts;
    }

    private static int travelMinutesBetween(PointOfInterest from, PointOfInterest to) {
        return TravelTimeCalculator.travelMinutes(from.getLat(), from.getLng(), to.getLat(), to.getLng());
    }

    private static String poiName(Stop stop) {
        return stop.getPointOfInterest() != null ? stop.getPointOfInterest().getName() : "unknown";
    }
}
