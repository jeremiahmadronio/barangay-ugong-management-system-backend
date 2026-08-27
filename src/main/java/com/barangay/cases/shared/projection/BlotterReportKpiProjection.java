package com.barangay.cases.shared.projection;

/**
 * Spring Data JPA Projection interface for Blotter Report KPI aggregation metrics.
 */
public interface BlotterReportKpiProjection {
    Number getTotalCases();
    Number getSettledCases();
    Number getActiveMediations();
    Number getLuponReferrals();
}
