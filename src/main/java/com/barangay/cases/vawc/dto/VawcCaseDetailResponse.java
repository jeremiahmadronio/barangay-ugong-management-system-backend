package com.barangay.cases.vawc.dto;

import com.barangay.zshared.dto.*;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.FilingType;

import java.time.LocalDateTime;
import java.util.List;

public record VawcCaseDetailResponse(
        Long id,
        String caseNumber,
        FilingType filingType,
        CaseStatus caseStatus,
        String caseStatusRemarks,
        String dateFiledFormatted,
        Long assignedOfficerId,
        String assignedOfficer,
        ComplainantDetailDto complainant,
        RespondentDetailDto respondent,
        IncidentDetailDto incident,
        String violenceType,
        NarrativeDetailDto narrative,
        EvidenceDetailDto evidence,
        List<EvidenceDetailDto> evidences,
        List<WitnessDetailDto> witnesses,
        List<ChildInvolvedDetailDto> children,
        String riskLevel,
        String dangerAssessments,
        String immediateRequests,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
