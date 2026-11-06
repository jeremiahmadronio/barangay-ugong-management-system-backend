package com.barangay.cases.bcpc.dto;

import com.barangay.zshared.dto.ComplainantDetailDto;
import com.barangay.zshared.dto.EvidenceDetailDto;
import com.barangay.zshared.dto.IncidentDetailDto;
import com.barangay.zshared.dto.NarrativeDetailDto;
import com.barangay.zshared.dto.WitnessDetailDto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Complete detail response payload for a Child in Conflict with the Law (CICL) case under BCPC.
 */
public record BcpcCiclCaseDetailResponse(
        Long caseId,
        String caseNumber,
        String filingType,
        String caseStatus,
        String caseStatusRemarks,
        String riskLevel,
        LocalDateTime createdAt,
        String reportingOfficer,
        String assignedOfficer,

        // Victim / Complainant Information
        ComplainantDetailDto victim,
        Boolean isVictimlessOffense,
        String victimRole,
        String victimRelationshipToChild,

        // CICL Child Information (Respondent)
        BcpcPersonDetailDto child,
        String gradeLevel,
        String schoolName,

        // Parent / Guardian Information
        BcpcPersonDetailDto guardian,
        String guardianRelationship,
        Boolean isGuardianPresentAtIntake,
        String guardianUnavailableReason,

        // CICL Assessment & Routing
        BcpcCiclAssessmentDto ciclAssessment,

        // Incident Details
        IncidentDetailDto incident,

        // Narrative / Statement of Facts
        NarrativeDetailDto narrative,

        // Supporting Documents / Evidence
        List<EvidenceDetailDto> evidences,

        // Witnesses
        List<WitnessDetailDto> witnesses,

        // Certification
        Boolean isAttested,
        LocalDateTime attestedAt
) {}

