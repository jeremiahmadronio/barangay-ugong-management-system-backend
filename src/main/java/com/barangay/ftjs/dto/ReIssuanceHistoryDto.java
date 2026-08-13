package com.barangay.ftjs.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReIssuanceHistoryDto(
        Long id,
        Integer issuanceNumber,   // e.g., 2 -> "Re-issuance #2"
        LocalDate dateOfLoss,
        String reason,           // e.g., "df"
        String orNumber,
        byte[] affidavitFiles,
        LocalDateTime requestedAt, // e.g., "Aug 13, 2026"
        String requestedBy
) {}