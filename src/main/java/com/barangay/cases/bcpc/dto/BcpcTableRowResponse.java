package com.barangay.cases.bcpc.dto;

import com.barangay.zshared.enumerated.CaseStatus;
import java.time.LocalDateTime;

public record BcpcTableRowResponse(
        Long id,
        String caseNumber,
        String childName,
        String caseNature,
        String category,
        CaseStatus status,
        String dateFiled,
        LocalDateTime createdAt
) {}

