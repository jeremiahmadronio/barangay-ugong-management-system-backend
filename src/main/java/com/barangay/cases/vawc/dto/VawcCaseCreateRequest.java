package com.barangay.cases.vawc.dto;

import com.barangay.zshared.dto.*;
import com.barangay.zshared.enumerated.FilingType;
import jakarta.validation.Valid;

import java.util.List;

/**
 * Request payload DTO for filing a VAWC Complaint (Intake / Case Filing).
 * Corresponds to Sections A to I of the VAWC Complaint Form.
 */
public record VawcCaseCreateRequest(
        // Section A: Case Information
        String caseNumber,
        Long assignedOfficerId,
        String assignedOfficer,

        // Section B: Complainant Information
        Long complainantId,
        @Valid ComplainantCreateDto complainant,

        // Section C: Respondent Information
        Long respondentId,
        @Valid RespondentCreateDto respondent,

        // Section D: Incident Details
        @Valid IncidentCreateDto incident,
        String violenceType,
        Long violenceTypeId,

        // Section E: Danger Assessment & Risk Level
        List<String> dangerAssessments,
        String otherDangerAssessment,
        String riskLevel, // LOW, MODERATE, HIGH-IMMINENT

        // Section F: Children Involved
        Boolean hasChildrenInvolved,
        List<ChildInvolvedCreateDto> children,

        // Section G: Method of Filing
        String filingMethod, // "ORAL_REPORT_ONLY", "WITH_WRITTEN_SWORN_STATEMENT"
        FilingType filingType,

        // Section H: Narrative / Sworn Statement
        @Valid NarrativeCreateDto narrative,

        // Section I: Evidence & Witnesses
        @Valid EvidenceCreateDto evidence,
        List<EvidenceCreateDto> evidences,
        String otherEvidenceDescription,
        List<WitnessCreateDto> witnesses,

        List<String> immediateRequests
) {
}
