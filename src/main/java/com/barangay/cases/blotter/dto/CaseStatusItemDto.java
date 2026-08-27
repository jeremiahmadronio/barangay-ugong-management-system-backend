package com.barangay.cases.blotter.dto;

import com.barangay.zshared.enumerated.CaseStatus;

/**
 * Data Transfer Object representing an individual case status metric.
 *
 * @param status     the case status enum
 * @param label      human-readable status name formatted for display
 * @param count      total number of cases under this status
 * @param percentage calculated percentage share against total cases
 */
public record CaseStatusItemDto(
        CaseStatus status,
        String label,
        long count,
        double percentage
) {}