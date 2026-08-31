package com.barangay.cases.shared.projection;

/**
 * Spring Data JPA Projection interface for Lupong Tagapamayapa Report KPI aggregation metrics.
 */
public interface LuponReportKpiProjection {
    Number getTotalReferred();
    Number getSettledCases();
    Number getActiveConciliations();
    Number getCfaIssued();
}
