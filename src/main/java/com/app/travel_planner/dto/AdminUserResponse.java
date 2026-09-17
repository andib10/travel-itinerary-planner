package com.app.travel_planner.dto;

import java.time.LocalDateTime;

/**
 * One row of {@code GET /api/admin/users}
 */
public record AdminUserResponse(
        Long id,
        String email,
        String role,
        LocalDateTime createdAt,
        int tripCount
) {
}
