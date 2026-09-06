package com.barangay.cases.vawc.dto;

import jakarta.validation.constraints.NotBlank;

public record ReferralFeedbackRequest(
        @NotBlank(message = "Feedback details are required")
        String details,

        Boolean resolvesCase,

        String resolutionType,

        String proofFileData,

        String proofFileName,

        Boolean declinedByAgency,

        String reasonForDecline
) {}
