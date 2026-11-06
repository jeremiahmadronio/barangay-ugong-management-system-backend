package com.barangay.cases.bcpc.dto;

import com.barangay.zshared.dto.EvidenceCreateDto;
import com.barangay.zshared.dto.IncidentCreateDto;
import com.barangay.zshared.dto.NarrativeCreateDto;
import com.barangay.zshared.dto.WitnessCreateDto;
import com.barangay.zshared.enumerated.CiclPathway;
import com.barangay.zshared.enumerated.DiscernmentStatus;
import com.barangay.zshared.enumerated.DiversionAuthority;
import com.barangay.zshared.enumerated.PenaltyRoutingCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request payload for creating a new Child in Conflict with the Law (CICL) case under BCPC.
 * Separated cleanly from CAR to enforce legal semantics (child is respondent/minor in conflict, victim is complainant).
 */
public record BcpcCiclCaseCreateRequest(
        String caseNumber,
        Long assignedOfficerId,
        String assignedOfficer,

        // Victim / Complainant Information
        Boolean isVictimlessOffense,
        @Valid
        BcpcPersonInputDto victim,
        String victimRole,
        String victimRelationshipToChild,

        // CICL Child Information (Respondent Minor)
        @NotNull(message = "Child information is required")
        @Valid
        BcpcPersonInputDto child,
        String gradeLevel,
        String schoolName,

        // Parent / Guardian Information
        Boolean isGuardianAvailable,
        BcpcPersonInputDto guardian,
        String guardianRelationship,
        String guardianUnavailableReason,

        // Incident Details & Allegation
        @NotNull(message = "Incident details are required")
        @Valid
        IncidentCreateDto incident,
        @NotBlank(message = "Alleged violation / offense is required")
        String allegedViolation,

        // Legal Screening & Routing (RA 9344 / RA 10630)
        Integer ageAtIncident,
        CiclPathway ciclPathway,
        DiscernmentStatus discernmentStatus,
        PenaltyRoutingCategory penaltyRoutingCategory,
        DiversionAuthority diversionAuthority,
        Boolean isDiversionEligible,
        Boolean admittedOffense,
        Boolean isRepeatOffender,

        // Apprehension & Turnover Details
        String apprehendingOfficerName,
        String apprehendingOfficerOffice,
        String apprehendingOfficerContact,
        LocalDateTime apprehensionDateTime,
        String apprehensionPlace,

        // Narrative / Statement of Facts
        NarrativeCreateDto narrative,

        // Supporting Documents / Evidence
        List<EvidenceCreateDto> evidences,

        // Witnesses
        List<WitnessCreateDto> witnesses,

        // Certification
        Boolean certifiedTrue
) {}

