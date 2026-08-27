package com.barangay.cases.shared.projection;

/**
 * Projection interface for aggregated case status counts from the database.
 */
public interface CaseStatusCountProjection {

    /**
     * Retrieves the case status (supports both CaseStatus enum or String value from DB).
     *
     * @return status object
     */
    Object getStatus();

    /**
     * Retrieves the count of cases under this status.
     *
     * @return total count
     */
    Long getCount();
}