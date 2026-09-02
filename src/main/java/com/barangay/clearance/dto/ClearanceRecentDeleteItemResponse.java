package com.barangay.clearance.dto;

import com.barangay.zshared.enumerated.ClearanceStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object representing a soft-deleted Clearance row in the trash vault table.
 */
public record ClearanceRecentDeleteItemResponse(
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
        LocalDateTime deletedAt,
        LocalDateTime createdAt
) {
}
