package com.barangay.people.officer.dto;

import java.time.LocalDateTime;

/**
 * DTO representing a case handled by an officer.
 */
public record OfficerCaseHistoryResponse(
        Long id,
        String caseNumber,
        String department,
        String role,
        String incidentNature,
        String status,
        LocalDateTime issuedDate
) {}
