package com.barangay.clearance.dto;

import java.time.LocalDateTime;

/**
 * Record representing a previous clearance / certificate issuance entry for a resident.
 */
public record ClearanceIssuanceHistoryResponse(
        String certNumber,
        String certTitle,
        LocalDateTime issuedAt,
        String status
) {
}
