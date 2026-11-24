package com.barangay.cases.shared.projection;

/**
 * Spring Data JPA Projection consolidating Blotter KPI Statistics into a single SQL execution.
 */
public interface BlotterStatsAggregateProjection {

    Number getTotalCases();

    Number getCurrentTotal();

    Number getPrevTotal();

    Number getTotalForTheRecord();

    Number getCurrentForRecord();

    Number getPrevForRecord();

    Number getInProgressCases();

    Number getResolvedCases();

    Number getCurrentResolved();

    Number getPrevResolved();
}
