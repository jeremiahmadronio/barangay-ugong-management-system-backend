package com.barangay.cases.lupon.dto;

import com.barangay.zshared.enumerated.CaseStatus;
import java.time.LocalDateTime;

/**
 * LuponTableRowResponse.java
 *
 * DTO representing a single row in the Lupong Tagapamayapa Cases Table.
 */
public record LuponTableRowResponse(
        Long rootCaseId,
        String rootCaseNumber,
        Long latestActiveCaseId,
        String latestCaseNumber,
        String filingType,
        Integer cycleCount,
        Boolean isReopened,
        CaseStatus effectiveStatus,
        String complainantFullName,
        String respondentFullName,
        LocalDateTime dateFiled,
        LocalDateTime dateReferred
) {}
