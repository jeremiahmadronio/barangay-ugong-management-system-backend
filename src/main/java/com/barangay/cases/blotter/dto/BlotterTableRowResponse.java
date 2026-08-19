package com.barangay.cases.blotter.dto;

import com.barangay.zshared.enumerated.CaseStatus;

import java.time.LocalDateTime;

public record BlotterTableRowResponse(
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
    LocalDateTime dateFiled
) {}
