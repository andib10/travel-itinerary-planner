package com.app.travel_planner.exception;

/**
 * Thrown for an unrecognized or expired token — used by both email verification and password reset
 */
public class InvalidOrExpiredTokenException extends RuntimeException {

    public InvalidOrExpiredTokenException(String message) {
        super(message);
    }
}
