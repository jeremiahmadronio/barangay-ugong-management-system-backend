package com.barangay.clearance.dto;

import com.barangay.zshared.enumerated.ClearanceStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object representing an archived Clearance row in the vault table.
 */
public record ClearanceArchiveItemResponse(
        Long id,
        String certNumber,
        Long templateId,
        String certTitle,
        Long personId,
        String requestorName,
        String orNumber,
        String ctnNumber,
        String purpose,
        String archiveRemarks,
        ClearanceStatus status,
        LocalDate expiryDate,
        LocalDateTime archivedAt,
        LocalDateTime createdAt
) {
}
