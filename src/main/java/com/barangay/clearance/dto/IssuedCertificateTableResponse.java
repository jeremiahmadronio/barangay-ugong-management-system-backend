package com.barangay.clearance.dto;

import com.barangay.zshared.enumerated.ClearanceStatus;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data representation for Issued Certificates table with resident grouping and issuance history.
 */
public record IssuedCertificateTableResponse(
        Long id,
        String certNumber,
        Long templateId,
        String certTitle,
        Long personId,
        String requestorName,
        String orNumber,
        String ctnNumber,
        String purpose,
        ClearanceStatus status,
        LocalDate expiryDate,
        LocalDateTime createdAt,
        JsonNode fieldValues,
        int totalIssuances,
        List<IssuedCertificateHistoryItem> history
) {
}
