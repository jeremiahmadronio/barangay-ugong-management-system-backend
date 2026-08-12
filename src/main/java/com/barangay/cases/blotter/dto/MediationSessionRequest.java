package com.barangay.cases.blotter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request DTO for scheduling a new Mediation (or Conciliation) hearing session
 * on a given Blotter Case.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediationSessionRequest {

    /** ISO-8601 datetime for the start of the scheduled session. */
    private LocalDateTime scheduledStart;

    /** ISO-8601 datetime for the end of the scheduled session. */
    private LocalDateTime scheduledEnd;

    /** Venue where the mediation will be held (e.g. "Office of Kapitana"). */
    private String venue;

    /** Optional notes or agenda for this session. */
    private String notes;
}
