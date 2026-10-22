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
        LocalDateTime dateSubmitted,
        String gender,
        Integer age,
        String educationalAttainment,
        String institution,
        String releasedBy
) {
    public FtjsReportTableResponse(
            Long id,
            String applicationNo,
            String applicantName,
            Long issuanceCount,
            String purpose,
            FtjsStatus status,
            LocalDateTime dateSubmitted
    ) {
        this(id, applicationNo, applicantName, issuanceCount, purpose, status, dateSubmitted, null, null, null, null, null);
    }
}
