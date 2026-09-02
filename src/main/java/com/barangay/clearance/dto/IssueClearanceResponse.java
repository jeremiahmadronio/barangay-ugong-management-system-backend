package com.barangay.clearance.dto;

/**
 * Response DTO after issuing a clearance or certificate.
 */
public record IssueClearanceResponse(
        boolean success,
        String message,
        String certificateNumber,
        Long issuedId
) {}
