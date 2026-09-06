package com.barangay.cases.vawc.dto;

import java.time.LocalDateTime;

public record VawcReferralResponse(
        Long id,
        Long caseId,
        String caseNumber,
        String referralControlNumber,
        String referredTo,
        String subjectOfLitigations,
        String grounds,
        String status,
        String referredBy,
        LocalDateTime referralDate,
        ReferralFeedbackDto feedback,
        LocalDateTime createdAt
) {}
