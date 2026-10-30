package com.barangay.cases.vawc.dto;

import com.barangay.zshared.dto.EvidenceCreateDto;
import com.barangay.zshared.dto.IncidentCreateDto;
import com.barangay.zshared.dto.NarrativeCreateDto;
import com.barangay.zshared.dto.WitnessCreateDto;
import jakarta.validation.Valid;

import java.util.List;

/**
 * Request payload DTO for updating an existing VAWC Case.
 * Non-null fields are updated differentially.
 */
public record VawcCaseUpdateRequest(
        String relationshipToComplainant,
        Boolean currentlyLivingWithComplainant,
        String violenceType,
        List<String> dangerAssessments,
        String otherDangerAssessment,
        String riskLevel,

        @Valid IncidentCreateDto incident,
        @Valid NarrativeCreateDto narrative,
        List<EvidenceCreateDto> evidences,
        List<WitnessCreateDto> witnesses
) {}

