package com.barangay.clearance.dto;

import java.time.LocalDateTime;

/**
 * Record representing an involved case entry (Blotter, Lupon, VAWC, BCPC) for a resident.
 */
public record ClearanceCaseHistoryResponse(
        String caseNumber,
        String incidentNature,
        String role,
        LocalDateTime createdAt,
        String status
) {
}
