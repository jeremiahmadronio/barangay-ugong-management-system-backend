package com.barangay.cases.blotter.dto;

/**
 * Data Transfer Object containing category breakdown and percentage shares
 * for formal complaint resolutions.
 *
 * @param settledAtBlotter cases resolved directly at desk level
 * @param settledRate      percentage resolved directly at desk level (0.0 - 100.0)
 * @param referredToLupon  cases escalated to Lupong Tagapamayapa
 * @param referredRate     percentage escalated to Lupon (0.0 - 100.0)
 * @param pendingAtBlotter cases still pending at desk level
 * @param pendingRate      percentage pending at desk level (0.0 - 100.0)
 */
public record ResolutionBreakdownDto(
        long settledAtBlotter,
        double settledRate,
        long referredToLupon,
        double referredRate,
        long pendingAtBlotter,
        double pendingRate
) {}