package com.app.travel_planner.optimization;

import com.app.travel_planner.entity.PointOfInterest;
import com.app.travel_planner.entity.Stop;
import com.app.travel_planner.util.GeoUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Nearest-neighbor re-sequencing of a day's stops (not an exact TSP solver — unnecessary
 * for a day-sized list). The first stop stays fixed as the starting point; each
 * subsequent position is filled with whichever remaining stop is geographically closest.
 * Only {@code orderIndex} is touched; timing is fixed afterward by {@link ItineraryOptimizer}.
 */
public final class RouteSequencer {

    private RouteSequencer() {
    }

    /**
     * @param currentOrderStops stops with index 0 as the fixed starting point
     * @return stops in nearest-neighbor order with orderIndex reassigned (1..N)
     */
    public static List<Stop> resequence(List<Stop> currentOrderStops) {
        if (currentOrderStops.size() <= 1) {
            return new ArrayList<>(currentOrderStops);
        }

        List<Stop> remaining = new ArrayList<>(currentOrderStops.subList(1, currentOrderStops.size()));
        List<Stop> resequenced = new ArrayList<>(currentOrderStops.size());

        Stop current = currentOrderStops.get(0);
        resequenced.add(current);

        while (!remaining.isEmpty()) {
            Stop nearest = nearestTo(current, remaining);
            remaining.remove(nearest);
            resequenced.add(nearest);
            current = nearest;
        }

        for (int i = 0; i < resequenced.size(); i++) {
            resequenced.get(i).setOrderIndex(i + 1);
        }

        return resequenced;
    }

    private static Stop nearestTo(Stop from, List<Stop> candidates) {
        PointOfInterest fromPoi = from.getPointOfInterest();
        Stop nearest = null;
        double nearestDistanceMeters = Double.MAX_VALUE;

        for (Stop candidate : candidates) {
            PointOfInterest candidatePoi = candidate.getPointOfInterest();
            double distanceMeters = GeoUtils.haversineMeters(
                    fromPoi.getLat(), fromPoi.getLng(), candidatePoi.getLat(), candidatePoi.getLng());
            if (distanceMeters < nearestDistanceMeters) {
                nearestDistanceMeters = distanceMeters;
                nearest = candidate;
            }
        }

        return nearest;
    }
}
