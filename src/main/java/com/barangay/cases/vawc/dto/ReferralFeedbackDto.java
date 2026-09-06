package com.barangay.cases.vawc.dto;

import java.time.LocalDateTime;

public record ReferralFeedbackDto(
        Long id,
        String details,
        Boolean resolvesCase,
        String resolutionType,
        String proofFileName,
        String proofFileData,
        Boolean declinedByAgency,
        String reasonForDecline,
        LocalDateTime createdAt
) {}
