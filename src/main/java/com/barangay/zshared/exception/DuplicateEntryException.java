package com.barangay.zshared.exception;

/**
 * Exception thrown when an attempt is made to persist a record with conflicting unique fields.
 */
public class DuplicateEntryException extends RuntimeException {

    public DuplicateEntryException(String message) {
        super(message);
    }
}
