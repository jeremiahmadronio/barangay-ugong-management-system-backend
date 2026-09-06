package com.barangay.cases.vawc.dto;

/**
 * Breakdown model for Inter-Agency Referrals by Partner Agency (CSWD/DSWD, PNP-WCPD, PAO, Medical).
 */
public record VawcAgencyReferralBreakdownResponse(
        String agencyName,
        long totalReferrals,
        double percentage,
        long resolvedCount,
        long declinedCount,
        long pendingCount,
        double successRate
) {}
