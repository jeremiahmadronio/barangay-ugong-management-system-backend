package com.barangay.cases.shared.projection;

import com.barangay.zshared.enumerated.CaseStatus;

/**
 * Projection interface for aggregated case status counts from the database.
 */
public interface CaseStatusCountProjection {

    /**
     * Retrieves the case status.
     *
     * @return {@link CaseStatus} enum
     */
    CaseStatus getStatus();

    /**
     * Retrieves the count of cases under this status.
     *
     * @return total count
     */
    Long getCount();
}