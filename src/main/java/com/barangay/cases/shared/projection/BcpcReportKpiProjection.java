package com.barangay.cases.shared.projection;

/**
 * Spring Data JPA Projection interface for BCPC Report KPI aggregation metrics.
 */
public interface BcpcReportKpiProjection {
    Number getTotalCases();
    Number getSettledCases();
    Number getActiveInterventions();
    Number getAgencyReferrals();
}

