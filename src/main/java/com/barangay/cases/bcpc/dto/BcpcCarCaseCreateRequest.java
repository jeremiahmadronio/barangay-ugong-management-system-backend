package com.barangay.cases.bcpc.dto;

import com.barangay.zshared.dto.EvidenceCreateDto;
import com.barangay.zshared.dto.IncidentCreateDto;
import com.barangay.zshared.dto.NarrativeCreateDto;
import com.barangay.zshared.dto.WitnessCreateDto;
import com.barangay.zshared.enumerated.CarActCategory;
import com.barangay.zshared.enumerated.CarOccurrenceCategory;
import com.barangay.zshared.enumerated.SpecificCarAct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request payload for creating a new Child at Risk (CAR) case under BCPC.
 */
public record BcpcCarCaseCreateRequest(
        String caseNumber,
        Long assignedOfficerId,
        String assignedOfficer,

        // C1. Reporting / Referring Person
        @NotNull(message = "Reporter details are required")
        @Valid
        BcpcPersonInputDto reporter,
        String reporterRole,
        String reporterRelationshipToChild,

        // C2. CAR Child Information
        @NotNull(message = "Child details are required")
        @Valid
        BcpcPersonInputDto child,
        String gradeLevel,
        String schoolName,

        // D. Parent / Guardian Information
        Boolean isGuardianAvailable,
        BcpcPersonInputDto guardian,
        String guardianRelationship,
        String guardianUnavailableReason,

        // E. Incident Details & Classification
        @NotNull(message = "Incident details are required")
        @Valid
        IncidentCreateDto incident,
        CarActCategory carActCategory,
        SpecificCarAct specificCarAct,
        String specificCarActOther,
        CarOccurrenceCategory carOccurrenceCategory,
        Boolean hasSafetyWelfareConcerns,
        String safetyWelfareNotes,
        String riskLevel,

        // F. Narrative / Statement of Facts
        NarrativeCreateDto narrative,

        // G. Supporting Documents / Evidence
        List<EvidenceCreateDto> evidences,

        // H. Witnesses
        List<WitnessCreateDto> witnesses,

        // I. Certification
        Boolean certifiedTrue
) {}

