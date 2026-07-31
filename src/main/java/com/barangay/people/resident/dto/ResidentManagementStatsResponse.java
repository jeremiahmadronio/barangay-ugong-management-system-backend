package com.barangay.people.resident.dto;

import com.barangay.usermanagement.user.dto.StatMetric;

/**
 * Response payload for Resident Management dashboard metrics.
 */
public record ResidentManagementStatsResponse(
        StatMetric totalResidents,
        StatMetric headsOfFamily,
        StatMetric seniorCitizens,
        StatMetric residentsThisMonth
) {
}
