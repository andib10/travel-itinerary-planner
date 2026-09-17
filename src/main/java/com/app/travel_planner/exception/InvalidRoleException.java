package com.app.travel_planner.exception;

/**
 * Thrown when a role-update request specifies anything other than USER/ADMIN.
 */
public class InvalidRoleException extends RuntimeException {

    public InvalidRoleException(String role) {
        super("Invalid role: " + role + " (must be USER or ADMIN)");
    }
}
