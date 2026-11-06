package com.barangay.cases.bcpc.dto;

import com.barangay.zshared.enumerated.CiclPathway;
import com.barangay.zshared.enumerated.DiscernmentStatus;
import com.barangay.zshared.enumerated.DiversionAuthority;
import com.barangay.zshared.enumerated.PenaltyRoutingCategory;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing the assessment and legal routing of a Child in Conflict with the Law (CICL)
 * under Republic Act No. 9344 (Juvenile Justice and Welfare Act of 2006) as amended by RA 10630.
 */
public record BcpcCiclAssessmentDto(
        Integer ageAtIncident,
        CiclPathway ciclPathway,
        DiscernmentStatus discernmentStatus,
        PenaltyRoutingCategory penaltyRoutingCategory,
        DiversionAuthority diversionAuthority,
        Boolean isDiversionEligible,
        Boolean admittedOffense,
        Boolean isRepeatOffender,
        String allegedViolation,
        String apprehendingOfficerName,
        String apprehendingOfficerOffice,
        String apprehendingOfficerContact,
        LocalDateTime apprehensionDateTime,
        String apprehensionPlace
) {}

