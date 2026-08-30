package com.barangay.cases.lupon.dto;

import com.barangay.cases.blotter.dto.SessionFollowUpResponse;
import com.barangay.cases.blotter.dto.SessionMinutesResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO returned when listing or creating Conciliation hearing sessions
 * for a Lupon Case.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConciliationSessionResponse {

    private Long id;

    /** The case ID this session belongs to. */
    private Long caseId;

    /** Sequential hearing number. */
    private Short hearingNumber;

    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;

    /** Venue string, e.g. "Lupon", "Barangay Hall". */
    private String venue;

    /** Session status: SCHEDULED | COMPLETED | CANCELLED | RESCHEDULED | PENDING_MINUTES */
    private String status;

    private String notes;

    /** Hearing Type: MEDIATION | CONCILIATION */
    private String hearingType;

    /** Dynamic session title: e.g. "Mediation 1" or "Conciliation Hearing 1" */
    private String sessionTitle;

    /** Whether this session belongs to Lupon conciliation phase */
    private Boolean isConciliation;

    private Boolean hasMinutes;
    private SessionMinutesResponse minutes;
    private List<SessionFollowUpResponse> followUps;

    /** Assigned Pangkat panel members and their attendance */
    private List<PanelAttendanceResponse> panelAttendances;

    private LocalDateTime createdAt;
}
