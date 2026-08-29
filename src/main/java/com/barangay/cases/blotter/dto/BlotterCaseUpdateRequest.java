package com.barangay.cases.blotter.dto;

import com.barangay.zshared.dto.EvidenceCreateDto;
import com.barangay.zshared.dto.IncidentCreateDto;
import com.barangay.zshared.dto.NarrativeCreateDto;
import com.barangay.zshared.dto.WitnessCreateDto;
import jakarta.validation.Valid;

import java.util.List;

/**
 * Request payload DTO for updating an existing Blotter Case.
 * Only non-null / provided fields are updated (differential update).
 */
public record BlotterCaseUpdateRequest(
        String relationshipToComplainant,
        Boolean currentlyLivingWithComplainant,

        @Valid IncidentCreateDto incident,
        @Valid NarrativeCreateDto narrative,
        List<EvidenceCreateDto> evidences,
        List<WitnessCreateDto> witnesses
) {}
