package com.app.travel_planner.dto;

/**
 * One row of {@code GET /api/admin/pois} — a POI plus data-quality flags.
 */
public record AdminPoiResponse(
        Long id,
        String xid,
        String name,
        String category,
        String city,
        Double lat,
        Double lng,
        String description,
        boolean hasDescription,
        boolean hasEmbedding,
        boolean hasOpeningHours
) {
}
