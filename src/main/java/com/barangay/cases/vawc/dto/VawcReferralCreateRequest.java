package com.barangay.cases.vawc.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record VawcReferralCreateRequest(
        @NotBlank(message = "Referred agency/destination is required")
        String referredTo,

        String subjectOfLitigations,

        String grounds,

        String referredBy,

        LocalDateTime referralDate
) {}
