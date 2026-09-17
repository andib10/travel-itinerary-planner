package com.app.travel_planner.dto;

import java.util.List;

/**
 * Raw deserialized shape of the LLM's itinerary JSON, before validation.
 */
public record GeneratedItineraryDraft(List<GeneratedDayDraft> days) {
}
