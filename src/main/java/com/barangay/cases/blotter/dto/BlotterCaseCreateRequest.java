package com.barangay.cases.blotter.dto;

import com.barangay.zshared.dto.*;
import com.barangay.zshared.enumerated.FilingType;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * Request payload DTO for creating a new Blotter Case entry (Formal Complaint or For the Record).
 */
public record BlotterCaseCreateRequest(
        FilingType filingType,
        String caseNumber,
        ComplainantDetailDto complainant,
        RespondentDetailDto respondent,

        String receivingOfficer,
        String assignedOfficer,
        IncidentDetailDto incident,
        NarrativeDetailDto narrative,
        EvidenceDetailDto evidence,
        List<WitnessDetailDto> witnesses,
        Boolean isCertified
) {

}
