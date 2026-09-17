package com.app.travel_planner.dto;

import com.app.travel_planner.optimization.OptimizationResult;

/**
 * Response for the day-optimize endpoint — updated stops plus what was fixed.
 */
public record OptimizeDayResponse(ItineraryDayResponse day, OptimizationResult optimization) {
}
