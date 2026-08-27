package com.barangay.cases.shared.projection;
/**
 * Spring Data JPA projection interface for retrieving monthly aggregated case counts.
 */
public interface CaseCountByMonthProjection {

    /**
     * Gets the year extracted from the creation timestamp.
     *
     * @return the year as an Integer
     */
    Integer getYear();
    /**
     * Gets the month numerical value (1-12) extracted from the creation timestamp.
     *
     * @return the month as an Integer
     */
    Integer getMonth();
    /**
     * Gets the total number of cases recorded in the specified month and year.
     *
     * @return the case count
     */
    Long getCount();
}
