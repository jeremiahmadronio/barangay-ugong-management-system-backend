package com.barangay.events.dto;

import java.time.LocalDateTime;

public record EventUpdateRequest(
        String title,
        LocalDateTime startDate,
        LocalDateTime endDate,
        LocalDateTime eventDate,
        Integer durationDays,
        String time,
        String location,
        String description,
        String category,
        Integer expectedAttendees,
        String status,
        String organizer,
        String contactPerson,
        String contactNumber,
        String requirements,
        String targetAudience,
        Boolean isFeatured
) {}
