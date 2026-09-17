package com.app.travel_planner.dto;

/**
 * One stop inside a {@link GeneratedDayDraft}, as returned by the LLM — unvalidated.
 */
public record GeneratedStopDraft(String poiId, String startTime, String endTime, String notes) {
}
