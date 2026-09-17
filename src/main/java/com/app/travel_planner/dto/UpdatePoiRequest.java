package com.app.travel_planner.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * {@code PUT /api/admin/pois/{id}} body. Partial update: a {@code null} field means "leave unchanged".
 */
@Getter
@Setter
public class UpdatePoiRequest {

    private String name;
    private String description;
    private String category;
}
