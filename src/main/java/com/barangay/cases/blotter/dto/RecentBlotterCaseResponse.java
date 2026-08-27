package com.barangay.cases.blotter.dto;

import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.FilingType;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing a newly filed Blotter case row on the dashboard.
 *
 * @param id              case database ID
 * @param caseNumber      official blotter case tracking number
 * @param complainantName full name of the complainant
 * @param respondentName  full name of the respondent
 * @param filingType      FORMAL_COMPLAINT or FOR_RECORD_ONLY
 * @param status          current status of the case
 * @param createdAt       exact timestamp when the case was filed today
 */
public record RecentBlotterCaseResponse(
        Long id,
        String caseNumber,
        String complainantName,
        String respondentName,
        FilingType filingType,
        CaseStatus status,
        LocalDateTime createdAt
) {}