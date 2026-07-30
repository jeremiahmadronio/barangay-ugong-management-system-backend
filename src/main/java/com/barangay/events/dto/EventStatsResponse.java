package com.barangay.events.dto;

public record EventStatsResponse(
        long upcomingEvents,
        long thisMonthEvents,
        long expectedAttendeesTotal,
        String nextEventTitle,
        String nextEventDate,
        long totalRegisteredAttendees,
        long activeDepartmentsCount
) {}
