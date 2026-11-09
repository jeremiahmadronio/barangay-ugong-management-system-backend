package com.barangay.cases.bcpc.dto;

import java.time.LocalDate;

/**
 * Response DTO comparing CAR (Children at Risk) vs CICL (Children in Conflict with the Law).
 */
public record BcpcCategoryComparisonResponse(
        LocalDate startDate,
        LocalDate endDate,
        long totalCases,
        long carCases,
        double carPercentage,
        long ciclCases,
        double ciclPercentage
) {
}

