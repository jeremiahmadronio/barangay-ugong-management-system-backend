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
        String caseStatusRemarks,
        String reopenRemarks,
        String caseNumber,
        String rootCaseNumber,
        Long rootCaseId,
        String previousCaseNumber,
        String reOpenedCaseNumber,
        String dateFiledFormatted,
        LocalDateTime rootCreatedAt,
        String rootDateFiledFormatted,
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
        List<CaseHistoryItemResponse> caseHistory,
        Boolean isCertified,
        Boolean isExtended,
        Integer extensionCount,
        LocalDateTime extendedAt,
        String extensionReason,
        String extendedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
