package com.app.travel_planner.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * {@code PUT /api/admin/users/{id}/role} body. {@code role} must be "USER" or "ADMIN".
 */
@Getter
@Setter
public class UpdateUserRoleRequest {

    @NotBlank
    private String role;
}
