package com.barangay.cases.bcpc.dto;

import com.barangay.zshared.dto.EvidenceCreateDto;
import com.barangay.zshared.dto.IncidentCreateDto;
import com.barangay.zshared.dto.NarrativeCreateDto;
import com.barangay.zshared.dto.WitnessCreateDto;
import com.barangay.zshared.enumerated.DiscernmentStatus;
import com.barangay.zshared.enumerated.DiversionAuthority;
import com.barangay.zshared.enumerated.PenaltyRoutingCategory;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO for updating an existing Child in Conflict with the Law (CICL) case.
 * Preserves respondent minor and victim identities while allowing selective updates
 * to incident details, relationship, legal screening, narrative, evidences, and witnesses.
 */
public record BcpcCiclCaseUpdateRequest(
        // Victim Relationship
        String victimRole,
        String victimRelationshipToChild,

        // Parent / Guardian & Education Status
        Boolean isGuardianAvailable,
        String guardianRelationship,
        String guardianUnavailableReason,
        BcpcPersonInputDto guardian,
        String gradeLevel,
        String schoolName,

        // Incident Details & Allegation
        @Valid IncidentCreateDto incident,
        String allegedViolation,

        // Legal Screening (RA 9344 / RA 10630)
        DiscernmentStatus discernmentStatus,
        PenaltyRoutingCategory penaltyRoutingCategory,
        DiversionAuthority diversionAuthority,
        Boolean admittedOffense,
        Boolean isRepeatOffender,

        // Apprehension Information
        String apprehendingOfficerName,
        String apprehendingOfficerOffice,
        String apprehendingOfficerContact,
        LocalDateTime apprehensionDateTime,
        String apprehensionPlace,

        // Narrative Statement
        @Valid NarrativeCreateDto narrative,

        // Supporting Documents & Testimonies
        List<EvidenceCreateDto> evidences,
        List<WitnessCreateDto> witnesses
) {}

