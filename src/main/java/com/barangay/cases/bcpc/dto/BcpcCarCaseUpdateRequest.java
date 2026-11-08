package com.barangay.cases.bcpc.dto;

import com.barangay.zshared.dto.EvidenceCreateDto;
import com.barangay.zshared.dto.IncidentCreateDto;
import com.barangay.zshared.dto.NarrativeCreateDto;
import com.barangay.zshared.dto.WitnessCreateDto;
import com.barangay.zshared.enumerated.CarActCategory;
import com.barangay.zshared.enumerated.CarCaseBasis;
import com.barangay.zshared.enumerated.CarOccurrenceCategory;
import com.barangay.zshared.enumerated.SpecificCarAct;
import jakarta.validation.Valid;

import java.util.List;

/**
 * Request DTO for updating an existing Child at Risk (CAR) case.
 * Preserves child and complainant identities while allowing selective updates
 * to incident details, guardian status, assessment, narrative, evidences, and witnesses.
 */
public record BcpcCarCaseUpdateRequest(
        // Reporter Relationship & Role
        String reporterRole,
        String reporterRelationshipToChild,

        // Parent / Guardian & Education Status
        Boolean isGuardianAvailable,
        String guardianRelationship,
        String guardianUnavailableReason,
        BcpcPersonInputDto guardian,
        String gradeLevel,
        String schoolName,

        // Incident Details
        @Valid IncidentCreateDto incident,

        // CAR Specific Legal Assessment
        CarCaseBasis carCaseBasis,
        CarActCategory carActCategory,
        SpecificCarAct specificCarAct,
        String specificCarActOther,
        CarOccurrenceCategory carOccurrenceCategory,
        Boolean hasAdditionalProtectionConcern,
        String safetyNotes,
        String additionalNotes,

        // Narrative Statement
        @Valid NarrativeCreateDto narrative,

        // Supporting Documents & Testimonies
        List<EvidenceCreateDto> evidences,
        List<WitnessCreateDto> witnesses
) {}

