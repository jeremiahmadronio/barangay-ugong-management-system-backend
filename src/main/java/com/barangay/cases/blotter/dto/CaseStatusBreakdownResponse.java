package com.barangay.cases.blotter.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Data Transfer Object representing the complete case status breakdown report.
 *
 * @param startDate    the start date filter
 * @param endDate      the end date filter
 * @param departmentId target department identifier (null if global overview)
 * @param totalCases   sum of all cases within the filtered scope
 * @param breakdown    list of status metrics for chart rendering
 */
public record CaseStatusBreakdownResponse(
        LocalDate startDate,
        LocalDate endDate,
        Long departmentId,
        long totalCases,
        List<CaseStatusItemDto> breakdown
) {}