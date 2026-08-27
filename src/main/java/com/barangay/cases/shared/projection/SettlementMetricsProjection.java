package com.barangay.cases.shared.projection;

/**
 * Projection interface for aggregated settlement performance metrics from database queries.
 */
public interface SettlementMetricsProjection {

    /**
     * Retrieves the total number of non-deleted cases filed within the period.
     *
     * @return total count of cases
     */
    Number getTotalCases();

    /**
     * Retrieves the total number of cases filed as formal complaints.
     *
     * @return count of formal complaints
     */
    Number getTotalFormalComplaints();

    /**
     * Retrieves the total number of cases marked with SETTLED status.
     *
     * @return count of settled cases
     */
    Number getTotalSettledCases();
}