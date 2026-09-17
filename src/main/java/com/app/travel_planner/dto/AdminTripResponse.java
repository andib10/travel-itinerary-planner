package com.app.travel_planner.dto;

import java.time.LocalDate;

/**
 * One row of {@code GET /api/admin/trips} — summary only, across every user's trips.
 */
public record AdminTripResponse(
        Long id,
        String ownerEmail,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        int dayCount
) {
}
