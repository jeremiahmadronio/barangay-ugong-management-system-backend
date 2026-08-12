package com.barangay.cases.blotter.dto;

import com.barangay.zshared.dto.*;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.FilingType;

import java.time.LocalDateTime;
import java.util.List;

public record BlotterCaseDetailResponse(
        Long id,
        FilingType filingType,
        CaseStatus caseStatus,
        String caseNumber,
        String dateFiledFormatted,
        Long receivingOfficerId,
        String receivingOfficer,
        Long assignedOfficerId,
        String assignedOfficer,
        ComplainantDetailDto complainant,
        RespondentDetailDto respondent,
        IncidentDetailDto incident,
        NarrativeDetailDto narrative,
        EvidenceDetailDto evidence,
        List<EvidenceDetailDto> evidences,
        List<WitnessDetailDto> witnesses,
        Boolean isCertified,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
