package com.barangay.ftjs.dto;

import com.barangay.zshared.enumerated.FtjsStatus;

import java.time.LocalDateTime;

/**
 * DTO response for FTJS analytics tabular report entries.
 */
public record FtjsReportTableResponse(
        Long id,
        String applicationNo,
        String applicantName,
        Long issuanceCount,
        String purpose,
        FtjsStatus status,
        LocalDateTime dateSubmitted
) {}
