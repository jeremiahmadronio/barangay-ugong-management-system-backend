package com.barangay.cases.blotter.dto;

import com.barangay.zshared.enumerated.CaseStatus;

/**
 * DTO representing an item in a case family's reopen history chain.
 *
 * @param id ID of the case record
 * @param caseNumber case number string (e.g. 2026-BLT-003)
 * @param dateFiledFormatted formatted filing date
 * @param status case status enum
 * @param isCurrent true if this case is currently being viewed
 * @param isPast true if this case is a past closed/dismissed cycle
 * @param isLatestActive true if this case is the latest active cycle in the lineage
 */
public record CaseHistoryItemResponse(
        Long id,
        String caseNumber,
        String dateFiledFormatted,
        CaseStatus status,
        Boolean isCurrent,
        Boolean isPast,
        Boolean isLatestActive
) {}
