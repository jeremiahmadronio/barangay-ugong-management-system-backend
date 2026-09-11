package com.barangay.people.resident.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Summary DTO of an Issued Clearance / Certificate for a specific resident.
 */
public record ResidentClearanceHistoryResponse(
        Long clearanceId,
        String certNumber,
        String clearanceType,
        String orNumber,
        String ctnNumber,
        BigDecimal amountPaid,
        Boolean isFree,
        String status,
        LocalDate expiryDate,
        LocalDateTime issuedAt
) {
}
