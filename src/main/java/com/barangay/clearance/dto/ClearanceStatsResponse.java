package com.barangay.clearance.dto;

/**
 * KPI stats for Clearance and Certificate Issuance dashboard.
 */
public record ClearanceStatsResponse(
        long totalIssued,
        long issuedThisMonth,
        String mostIssuedCert,
        String mostCommonPurpose
) {
}
