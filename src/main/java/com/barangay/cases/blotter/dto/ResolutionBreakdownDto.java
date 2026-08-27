package com.barangay.cases.blotter.dto;

/**
 * Data Transfer Object containing category breakdown and percentage shares
 * for formal complaint resolutions.
 *
 * @param settledAtBlotter         cases resolved directly at desk level
 * @param settledAtBlotterRate     percentage resolved directly at desk level (0.0 - 100.0)
 * @param referredToLupon          cases escalated to Lupong Tagapamayapa
 * @param referredToLuponRate      percentage escalated to Lupon (0.0 - 100.0)
 * @param unresolvedOrPending      cases still pending at desk level
 * @param unresolvedOrPendingRate  percentage pending at desk level (0.0 - 100.0)
 */
public record ResolutionBreakdownDto(
        long settledAtBlotter,
        double settledAtBlotterRate,
        long referredToLupon,
        double referredToLuponRate,
        long unresolvedOrPending,
        double unresolvedOrPendingRate
) {}