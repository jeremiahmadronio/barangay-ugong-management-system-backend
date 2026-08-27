package com.barangay.cases.shared.projection;

import java.time.LocalDate;
/**
 * Spring Data JPA projection interface for retrieving daily aggregated case counts.
 */
public interface CaseCountByDayProjection {
    /**
     * Gets the specific calendar date extracted from the creation timestamp.
     *
     * @return the {@link LocalDate} of the aggregation
     */
    LocalDate getDate();
    /**
     * Gets the total number of cases recorded on the specified date.
     *
     * @return the case count
     */
    Long getCount();
}
