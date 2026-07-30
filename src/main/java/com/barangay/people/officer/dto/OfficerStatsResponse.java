package com.barangay.people.officer.dto;

public record OfficerStatsResponse(
        long totalOfficers,
        String totalOfficersTrend, // e.g. "+12% vs last month" or "vs last month"
        long activeOfficers,
        long inactiveStaff,
        long departmentsCount
) {}
