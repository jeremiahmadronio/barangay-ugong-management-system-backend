package com.barangay.cases.bcpc.dto;

import java.time.LocalDateTime;

public record BcpcTableRowResponse(
        Long id,
        String caseNumber,
        String childName,
        String caseNature,
        String category,
        String status,
        String dateFiled,
        LocalDateTime createdAt
) {}

