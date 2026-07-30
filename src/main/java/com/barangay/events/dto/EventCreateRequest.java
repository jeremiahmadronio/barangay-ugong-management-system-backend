package com.barangay.events.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record EventCreateRequest(
        @NotBlank(message = "Event title is required")
        String title,

        LocalDateTime startDate,

        LocalDateTime endDate,

        LocalDateTime eventDate,

        Integer durationDays,

        String time,

        @NotBlank(message = "Event location is required")
        String location,

        @NotBlank(message = "Description is required")
        String description,

        @NotBlank(message = "Category is required")
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
