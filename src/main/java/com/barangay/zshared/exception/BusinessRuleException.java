package com.barangay.zshared.exception;

/**
 * Exception thrown when an operation violates domain or workflow business constraints.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
