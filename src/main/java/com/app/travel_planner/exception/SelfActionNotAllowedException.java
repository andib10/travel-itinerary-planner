package com.app.travel_planner.exception;

/**
 * Thrown when an admin tries to change their own role or delete their own account.
 */
public class SelfActionNotAllowedException extends RuntimeException {

    public SelfActionNotAllowedException(String message) {
        super(message);
    }
}
