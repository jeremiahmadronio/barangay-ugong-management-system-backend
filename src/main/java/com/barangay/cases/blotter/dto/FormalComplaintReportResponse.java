package com.barangay.cases.blotter.dto;

import java.time.LocalDate;

/**
 * Top-level response payload for formal complaint resolution and escalation analytics.
 *
 * @param startDate              start date filter boundary
 * @param endDate                end date filter boundary
 * @param departmentId           target department identifier (null if global)
 * @param totalFormalComplaints  total formal complaints within date range
 * @param resolutionBreakdown    granular breakdown metrics
 * @param overallSettlementRate  desk settlement rate percentage
 */
public record FormalComplaintReportResponse(
        LocalDate startDate,
        LocalDate endDate,
        Long departmentId,
        long totalFormalComplaints,
        ResolutionBreakdownDto resolutionBreakdown,
        double overallSettlementRate
) {}