package com.barangay.cases.blotter.dto;

import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.FilingType;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing a soft-deleted Blotter case row in the recent delete table.
 *
 * @param id              case database ID
 * @param caseNumber      official blotter case tracking number
 * @param complainantName full name of the complainant
 * @param respondentName  full name of the respondent
 * @param status          final status before soft delete
 * @param deletedAt       timestamp when the record was soft-deleted
 * @param filingType      filing type (FORMAL_COMPLAINT or FOR_RECORD_ONLY)
 */
public record BlotterRecentDeleteItemResponse(
        Long id,
        String caseNumber,
        String complainantName,
        String respondentName,
        CaseStatus status,
        LocalDateTime deletedAt,
        FilingType filingType
) {}
