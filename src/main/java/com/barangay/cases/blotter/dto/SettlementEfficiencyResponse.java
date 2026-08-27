package com.barangay.cases.blotter.dto;

import java.time.LocalDate;

/**
 * Data Transfer Object representing the calculated settlement efficiency report.
 *
 * @param startDate              the start boundary of the report
 * @param endDate                the end boundary of the report
 * @param departmentId           the target department identifier (null if global)
 * @param totalCases             total cases created within the date range
 * @param totalFormalComplaints  total cases filed as FORMAL_COMPLAINT
 * @param totalSettledCases      total cases marked as SETTLED
 * @param settlementRate         the calculated settlement percentage rate (0.0 to 100.0)
 */
public record SettlementEfficiencyResponse(
        LocalDate startDate,
        LocalDate endDate,
        Long departmentId,
        long totalCases,
        long totalFormalComplaints,
        long totalSettledCases,
        double settlementRate
) {}