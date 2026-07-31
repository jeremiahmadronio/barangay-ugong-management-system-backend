package com.barangay.usermanagement.user.dto;

/**
 * Response payload for User Management dashboard metrics.
 */
public record UserManagementStatsResponse(
        StatMetric totalUsers,
        StatMetric activeUsers,
        StatMetric lockedUsers,
        StatMetric inactiveUsers
) {
}
