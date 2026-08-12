package com.barangay.cases.blotter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineEventResponse {
    private Long id;
    private Long caseId;
    private String eventType;
    private String title;
    private String description;
    private String performedBy;
    private LocalDateTime eventDate;
}
