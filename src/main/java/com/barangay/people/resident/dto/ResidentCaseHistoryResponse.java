package com.barangay.people.resident.dto;

import java.time.LocalDateTime;

/**
 * Summary DTO of a legal / barangay case involving a specific resident.
 */
public record ResidentCaseHistoryResponse(
        Long caseId,
        String caseNumber,
        String department,
        String role,
        String incidentNature,
        String status,
        LocalDateTime dateFiled
) {
}
