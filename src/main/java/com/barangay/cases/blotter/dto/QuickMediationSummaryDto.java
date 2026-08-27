package com.barangay.cases.blotter.dto;

import java.time.LocalDateTime;

/**
 * Ultra-lightweight summary DTO for dashboard quick-view cards.
 */
public record QuickMediationSummaryDto(
        Long sessionId,
        String caseNumber,
        String partiesSummary,     // Format: "Juan Dela Cruz vs. Pedro Penduko"
        Short summonNumber,
        LocalDateTime scheduledStart,
        String venue
) {}