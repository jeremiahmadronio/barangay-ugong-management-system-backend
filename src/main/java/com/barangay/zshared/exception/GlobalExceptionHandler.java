package com.barangay.zshared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized exception handler for REST controllers mapping exceptions to standardized HTTP responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles resource not found exceptions, returning 404 Not Found.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody(ex.getMessage()));
    }

    /**
     * Handles bad request exceptions, returning 400 Bad Request.
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody(ex.getMessage()));
    }

    /**
     * Handles duplicate entry conflicts, returning 409 Conflict.
     */
    @ExceptionHandler(DuplicateEntryException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(DuplicateEntryException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorBody(ex.getMessage()));
    }

    /**
     * Handles business rule violations, returning 422 Unprocessable Entity.
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, String>> handleBusinessRule(BusinessRuleException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(errorBody(ex.getMessage()));
    }

    /**
     * Handles unauthorized access attempts, returning 403 Forbidden.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(errorBody(ex.getMessage()));
    }

    /**
     * Handles invalid authentication credentials, returning 400 Bad Request.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody(ex.getMessage()));
    }

    /**
     * Handles disabled accounts, locked accounts, and missing user exceptions, returning 400 Bad Request.
     */
    @ExceptionHandler({
            DisabledException.class,
            LockedException.class,
            UsernameNotFoundException.class
    })
    public ResponseEntity<Map<String, String>> handleAuthExceptions(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody(ex.getMessage()));
    }

    /**
     * Handles bean validation failures, returning field error details with 400 Bad Request.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errors);
    }

    /**
     * Handles runtime state and illegal argument exceptions, returning 400 Bad Request.
     */
    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, String>> handleIllegalStateAndArgument(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody(ex.getMessage()));
    }

    /**
     * Handles generic unhandled exceptions, returning 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody("Something went wrong: " + ex.getMessage()));
    }

    /**
     * Builds a structured error response payload.
     */
    private Map<String, String> errorBody(String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", message);
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now().toString());
        return body;
    }
}
