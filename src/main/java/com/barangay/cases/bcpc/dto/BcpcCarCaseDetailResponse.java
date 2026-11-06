package com.barangay.cases.bcpc.dto;

import com.barangay.zshared.dto.ComplainantDetailDto;
import com.barangay.zshared.dto.EvidenceDetailDto;
import com.barangay.zshared.dto.IncidentDetailDto;
import com.barangay.zshared.dto.NarrativeDetailDto;
import com.barangay.zshared.dto.WitnessDetailDto;
import com.barangay.zshared.enumerated.CarActCategory;
import com.barangay.zshared.enumerated.CarOccurrenceCategory;
import com.barangay.zshared.enumerated.SpecificCarAct;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Complete detail response payload for a Child at Risk (CAR) case under BCPC.
 */
public record BcpcCarCaseDetailResponse(
        Long caseId,
        String caseNumber,
        String filingType,
        String caseStatus,
        String caseStatusRemarks,
        String riskLevel,
        LocalDateTime createdAt,
        String reportingOfficer,
        String assignedOfficer,

        // C1. Reporter / Referring Person
        ComplainantDetailDto reporter,
        String reporterRole,
        String reporterRelationshipToChild,

        // C2. CAR Child Information
        BcpcPersonDetailDto child,
        String gradeLevel,
        String schoolName,

        // D. Parent / Guardian Information
        BcpcPersonDetailDto guardian,
        String guardianRelationship,
        Boolean isGuardianPresentAtIntake,
        String guardianUnavailableReason,

        // E. Incident Details & Classification
        IncidentDetailDto incident,
        CarActCategory carActCategory,
        SpecificCarAct specificCarAct,
        String specificCarActOther,
        CarOccurrenceCategory carOccurrenceCategory,
        Boolean needsProtectiveCustody,
        String temporaryShelterPlacement,
        String safetyWelfareNotes,

        // F. Narrative / Statement of Facts
        NarrativeDetailDto narrative,

        // G. Supporting Documents / Evidence
        List<EvidenceDetailDto> evidences,

        // H. Witnesses
        List<WitnessDetailDto> witnesses,

        // I. Certification
        Boolean isAttested,
        LocalDateTime attestedAt
) {}

