package com.barangay.people.resident.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Summary DTO of an FTJS Certificate issued to a specific resident.
 */
public record ResidentFtjsHistoryResponse(
        Long ftjsId,
        String ftjsNumber,
        String purpose,
        String educationalAttainment,
        String schoolInstitution,
        Integer issuanceCount,
        String status,
        LocalDate dateSubmitted,
        LocalDateTime issuedAt
) {
}
