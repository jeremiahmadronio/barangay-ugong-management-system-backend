package com.barangay.clearance.dto;

import com.barangay.zshared.enumerated.ClearanceStatus;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Individual certificate issuance record in a resident's history timeline.
 */
public record IssuedCertificateHistoryItem(
        Long id,
        String certNumber,
        Long templateId,
        String certTitle,
        String orNumber,
        String ctnNumber,
        String purpose,
        ClearanceStatus status,
        LocalDate expiryDate,
        LocalDateTime createdAt,
        JsonNode fieldValues
) {
}
