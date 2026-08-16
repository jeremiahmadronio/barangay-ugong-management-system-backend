package com.barangay.ftjs.dto;

import com.barangay.zshared.enumerated.FtjsStatus;

import java.time.LocalDateTime;

/**
 * DTO summary response for FTJS management data table display.
 */
public record FtjsSummaryResponse (
        Long id,
        String trackingNumber,
        String fullName,
        Long issuanceCount,
        FtjsStatus status,
        String purpose,
        LocalDateTime dateSubmitted
) {
}
