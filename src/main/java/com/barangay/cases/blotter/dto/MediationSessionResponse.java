package com.barangay.cases.blotter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO returned when listing or creating Mediation hearing sessions
 * for a Blotter Case.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediationSessionResponse {

    private Long id;

    /** The blotter case this session belongs to. */
    private Long caseId;

    /** Sequential hearing number (1, 2, 3…) — maps to summonNumber. */
    private Short hearingNumber;

    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;

    /** Venue string, e.g. "Office of Kapitana", "Lupon", "Barangay Hall". */
    private String venue;

    /** Session status: SCHEDULED | COMPLETED | CANCELLED | RESCHEDULED | PENDING_MINUTES */
    private String status;

    private String notes;

    private Boolean hasMinutes;
    private SessionMinutesResponse minutes;
    private java.util.List<SessionFollowUpResponse> followUps;

    private LocalDateTime createdAt;
}


