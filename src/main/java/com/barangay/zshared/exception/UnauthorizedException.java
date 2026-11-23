package com.barangay.zshared.exception;

/**
 * Exception thrown when an authenticated user does not have permission to access a resource.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
