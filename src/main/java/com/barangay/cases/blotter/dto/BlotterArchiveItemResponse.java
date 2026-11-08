package com.barangay.cases.blotter.dto;

import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.FilingType;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing an archived Blotter case row in the table.
 *
 * @param id              case database ID
 * @param caseNumber      official blotter case tracking number
 * @param complainantName full name of the complainant
 * @param respondentName  full name of the respondent
 * @param archiveRemarks  reason or remarks upon archiving the case
 * @param status          final status before or upon archiving
 * @param archivedAt      timestamp when the record was archived
 * @param filingType      filing type (FORMAL_COMPLAINT or FOR_RECORD_ONLY)
 */
public record BlotterArchiveItemResponse(
        Long id,
        String caseNumber,
        String complainantName,
        String respondentName,
        String archiveRemarks,
        CaseStatus status,
        LocalDateTime archivedAt,
        FilingType filingType,
        String classification
) {
    public BlotterArchiveItemResponse(
            Long id,
            String caseNumber,
            String complainantName,
            String respondentName,
            String archiveRemarks,
            CaseStatus status,
            LocalDateTime archivedAt,
            FilingType filingType
    ) {
        this(id, caseNumber, complainantName, respondentName, archiveRemarks, status, archivedAt, filingType, null);
    }
}