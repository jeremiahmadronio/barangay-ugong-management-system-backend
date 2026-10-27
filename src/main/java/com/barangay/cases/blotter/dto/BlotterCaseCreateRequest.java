package com.barangay.cases.blotter.dto;

import com.barangay.zshared.dto.*;
import com.barangay.zshared.enumerated.FilingType;
import jakarta.validation.Valid;

import java.util.List;

/**
 * Request payload DTO for creating a new Blotter Case entry (Formal Complaint or For the Record).
 */
public record BlotterCaseCreateRequest(
        FilingType filingType,
        String caseNumber,
        @Valid ComplainantCreateDto complainant,
        @Valid RespondentCreateDto respondent,

        Long receivingOfficerId,
        String receivingOfficer,
        Long assignedOfficerId,
        String assignedOfficer,

        @Valid IncidentCreateDto incident,
        @Valid NarrativeCreateDto narrative,
        @Valid EvidenceCreateDto evidence,
        List<EvidenceCreateDto> evidences,
        List<WitnessCreateDto> witnesses,
        Boolean isCertified,
        Long ftjsApplicationId,
        String relatedBarangayService
) {
    public BlotterCaseCreateRequest(
            FilingType filingType,
            String caseNumber,
            ComplainantCreateDto complainant,
            RespondentCreateDto respondent,
            Long receivingOfficerId,
            String receivingOfficer,
            Long assignedOfficerId,
            String assignedOfficer,
            IncidentCreateDto incident,
            NarrativeCreateDto narrative,
            EvidenceCreateDto evidence,
            List<EvidenceCreateDto> evidences,
            List<WitnessCreateDto> witnesses,
            Boolean isCertified
    ) {
        this(filingType, caseNumber, complainant, respondent, receivingOfficerId, receivingOfficer,
                assignedOfficerId, assignedOfficer, incident, narrative, evidence, evidences, witnesses, isCertified,
                null, null);
    }
}

