package com.barangay.cases.vawc.dto;

import com.barangay.zshared.enumerated.CaseStatus;
import java.time.LocalDateTime;

public record VawcTableRowResponse(
        Long id,
        String caseNumber,
        String complainant,
        String respondent,
        String natureOfComplaint,
        CaseStatus status,
        String riskLevel,
        String dateFiled,
        LocalDateTime createdAt
) {}
