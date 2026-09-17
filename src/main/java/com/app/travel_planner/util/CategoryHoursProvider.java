package com.app.travel_planner.util;

import java.time.LocalTime;
import java.util.Map;

/**
 * Default opening-hours windows per {@link com.app.travel_planner.entity.PointOfInterest#getCategory()}.
 * OpenTripMap has no structured opening-hours data, so these are reasonable approximations,
 * not per-venue ground truth. Unmapped categories fall back to {@link #OTHER}.
 */
public final class CategoryHoursProvider {

    private static final TimeWindow OTHER = new TimeWindow(LocalTime.of(8, 0), LocalTime.of(21, 0));

    private static final Map<String, TimeWindow> DEFAULTS = Map.ofEntries(
            Map.entry("museums", new TimeWindow(LocalTime.of(9, 0), LocalTime.of(18, 0))),
            Map.entry("historic", new TimeWindow(LocalTime.of(8, 0), LocalTime.of(20, 0))),
            Map.entry("religion", new TimeWindow(LocalTime.of(8, 0), LocalTime.of(19, 0))),
            Map.entry("restaurants", new TimeWindow(LocalTime.of(12, 0), LocalTime.of(23, 0))),
            Map.entry("cafes", new TimeWindow(LocalTime.of(8, 0), LocalTime.of(20, 0))),
            Map.entry("bars", new TimeWindow(LocalTime.of(17, 0), LocalTime.of(2, 0))),
            Map.entry("pubs", new TimeWindow(LocalTime.of(17, 0), LocalTime.of(1, 0))),
            Map.entry("cultural", new TimeWindow(LocalTime.of(9, 0), LocalTime.of(19, 0))),
            Map.entry("architecture", new TimeWindow(LocalTime.of(8, 0), LocalTime.of(20, 0))),
            Map.entry("other", OTHER)
    );

    private CategoryHoursProvider() {
    }

    /**
     * Default opening-hours window for a POI category; null/blank/unmapped resolve to {@link #OTHER}.
     */
    public static TimeWindow windowFor(String category) {
        if (category == null) {
            return OTHER;
        }
        return DEFAULTS.getOrDefault(category.trim().toLowerCase(), OTHER);
    }

    public static boolean isWithinHours(String category, LocalTime time) {
        return windowFor(category).contains(time);
    }
}
