package com.barangay.zshared.exception;

/**
 * Exception thrown when a requested entity cannot be found in the system.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
