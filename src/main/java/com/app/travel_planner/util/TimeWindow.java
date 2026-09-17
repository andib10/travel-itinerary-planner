package com.app.travel_planner.util;

import java.time.LocalTime;

/**
 * An opening-hours window. {@code close} may be earlier than {@code open} to represent
 * a window that spans midnight (e.g. a bar open 17:00-02:00).
 */
public record TimeWindow(LocalTime open, LocalTime close) {

    /**
     * Whether {@code time} falls within this window, inclusive of both endpoints.
     */
    public boolean contains(LocalTime time) {
        if (close.isBefore(open)) {
            // Spans midnight: valid if on evening side or early-morning side.
            return !time.isBefore(open) || !time.isAfter(close);
        }
        return !time.isBefore(open) && !time.isAfter(close);
    }
}
