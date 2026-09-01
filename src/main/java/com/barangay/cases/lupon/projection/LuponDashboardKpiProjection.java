package com.barangay.cases.lupon.projection;

/**
 * Spring Data Projection interface for aggregating Lupon operational dashboard metrics.
 */
public interface LuponDashboardKpiProjection {
    Long getTotalReferredThisWeek();
    Long getTotalUnderConciliation();
    Long getTotalCfaIssuedThisWeek();
}
