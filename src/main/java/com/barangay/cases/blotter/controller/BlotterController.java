package com.barangay.cases.blotter.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.barangay.cases.blotter.dto.BlotterCaseCreateRequest;
import com.barangay.cases.blotter.dto.BlotterCaseDetailResponse;
import com.barangay.cases.blotter.dto.BlotterFilterRequest;
import com.barangay.cases.blotter.dto.BlotterStatsResponse;
import com.barangay.cases.blotter.dto.CaseNoteRequest;
import com.barangay.cases.blotter.dto.CaseNoteResponse;
import com.barangay.cases.blotter.dto.MediationSessionRequest;
import com.barangay.cases.blotter.dto.MediationSessionResponse;
import com.barangay.cases.blotter.dto.SessionFollowUpRequest;
import com.barangay.cases.blotter.dto.SessionFollowUpResponse;
import com.barangay.cases.blotter.dto.SessionMinutesRequest;
import com.barangay.cases.blotter.dto.SessionMinutesResponse;
import com.barangay.cases.blotter.dto.TimelineEventResponse;
import com.barangay.cases.blotter.service.BlotterCaseService;
import com.barangay.cases.blotter.service.CaseFileService;
import com.barangay.cases.shared.service.CaseNoteService;
import com.barangay.cases.blotter.service.MediationSessionService;
import com.barangay.zshared.audit.annotations.LogAudit;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;
import com.barangay.zshared.mapper.PageResponse;

import lombok.RequiredArgsConstructor;

/**
 * REST Controller for Blotter Case Management.
 * Handles listing blotter entries with server-side pagination/filtering,
 * retrieving case details, calculating KPI stats, creating new blotter entries,
 * serving files, mediation sessions, case notes, and timeline events.
 */
@RestController
@RequestMapping("/api/v1/blotter")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class BlotterController {

    private final BlotterCaseService blotterCaseService;
    private final MediationSessionService mediationSessionService;
    private final CaseNoteService caseNoteService;
    private final CaseFileService caseFileService;

    /**
     * Retrieves paginated and filtered blotter cases.
     * Uses server-side pagination without findAll().
     *
     * @param filter request containing search, status, filingType, natureOfComplaint, page, size
     * @return {@link PageResponse} containing the paginated list of blotter cases
     */
    @GetMapping
    public ResponseEntity<PageResponse<BlotterCaseDetailResponse>> getBlotterCases(
            @ModelAttribute BlotterFilterRequest filter) {
        PageResponse<BlotterCaseDetailResponse> response = blotterCaseService.getBlotterCases(filter);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves KPI Statistics for Blotter cases with month-to-date vs full previous month trends.
     *
     * @return {@link BlotterStatsResponse} containing counts and trends
     */
    @GetMapping("/stats")
    public ResponseEntity<BlotterStatsResponse> getBlotterStats() {
        BlotterStatsResponse stats = blotterCaseService.getBlotterStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Retrieves a single blotter case detail by ID.
     *
     * @param id the case ID
     * @return {@link BlotterCaseDetailResponse}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BlotterCaseDetailResponse> getBlotterCaseById(@PathVariable Long id) {
        BlotterCaseDetailResponse response = blotterCaseService.getBlotterCaseById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new Blotter Case entry (Formal Complaint or For the Record).
     *
     * @param request the case creation request payload containing all case details and sub-entities
     * @return {@link ResponseEntity} containing the created case details with HTTP status 201 (Created)
     */
    @PostMapping
    @LogAudit(module = AuditModule.BLOTTER, action = "CREATE_BLOTTER_CASE", severity = Severity.INFO)
    public ResponseEntity<BlotterCaseDetailResponse> createBlotterCase(@Valid @RequestBody BlotterCaseCreateRequest request) {
        BlotterCaseDetailResponse response = blotterCaseService.createBlotterCase(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/files/narrative/{caseId}")
    public ResponseEntity<byte[]> getNarrativeFile(@PathVariable Long caseId) {
        return caseFileService.getNarrativeFile(caseId);
    }

    @GetMapping("/files/evidence/{evidenceId}")
    public ResponseEntity<byte[]> getEvidenceFile(@PathVariable Long evidenceId) {
        return caseFileService.getEvidenceFile(evidenceId);
    }

    @GetMapping("/files/witness/{witnessId}")
    public ResponseEntity<byte[]> getWitnessFile(@PathVariable Long witnessId) {
        return caseFileService.getWitnessFile(witnessId);
    }

    // -------------------------------------------------------------------------
    // Mediation Session Endpoints
    // -------------------------------------------------------------------------

    /**
     * Lists all scheduled mediation sessions for a given blotter case.
     *
     * @param caseId the blotter case ID
     * @return list of {@link MediationSessionResponse}
     */
    @GetMapping("/{caseId}/mediation")
    public ResponseEntity<List<MediationSessionResponse>> getMediationSessions(
            @PathVariable Long caseId) {
        return ResponseEntity.ok(mediationSessionService.getMediationSessions(caseId));
    }

    /**
     * Schedules a new mediation hearing for a given blotter case.
     *
     * @param caseId  the blotter case ID
     * @param request the schedule request payload
     * @return the created session with HTTP 201
     */
    @PostMapping("/{caseId}/mediation")
    public ResponseEntity<MediationSessionResponse> scheduleMediation(
            @PathVariable Long caseId,
            @Valid @RequestBody MediationSessionRequest request) {
        MediationSessionResponse response = mediationSessionService.scheduleMediation(caseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Cancels a scheduled mediation session.
     *
     * @param sessionId the session ID to cancel
     * @return the updated session
     */
    @PatchMapping("/mediation/{sessionId}/cancel")
    public ResponseEntity<MediationSessionResponse> cancelMediation(
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(mediationSessionService.cancelMediation(sessionId));
    }

    /**
     * Records or updates minutes for a mediation session.
     */
    @PostMapping("/mediation/{sessionId}/minutes")
    public ResponseEntity<MediationSessionResponse> recordSessionMinutes(
            @PathVariable Long sessionId,
            @Valid @RequestBody SessionMinutesRequest request) {
        return ResponseEntity.ok(mediationSessionService.recordSessionMinutes(sessionId, request));
    }

    /**
     * Gets recorded minutes for a mediation session.
     */
    @GetMapping("/mediation/{sessionId}/minutes")
    public ResponseEntity<SessionMinutesResponse> getMediationMinutes(
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(mediationSessionService.getMediationMinutes(sessionId));
    }

    /**
     * Adds a follow-up note to a mediation session.
     */
    @PostMapping("/mediation/{sessionId}/followup")
    public ResponseEntity<SessionFollowUpResponse> addSessionFollowUp(
            @PathVariable Long sessionId,
            @Valid @RequestBody SessionFollowUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mediationSessionService.addSessionFollowUp(sessionId, request));
    }

    /**
     * Lists all follow-up notes for a mediation session.
     */
    @GetMapping("/mediation/{sessionId}/followup")
    public ResponseEntity<List<SessionFollowUpResponse>> getSessionFollowUps(
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(mediationSessionService.getSessionFollowUps(sessionId));
    }

    /**
     * Adds a note to a blotter case.
     */
    @PostMapping("/{caseId}/notes")
    public ResponseEntity<CaseNoteResponse> addCaseNote(
            @PathVariable Long caseId,
            @Valid @RequestBody CaseNoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(caseNoteService.addCaseNote(caseId, request));
    }

    /**
     * Lists all notes for a blotter case.
     */
    @GetMapping("/{caseId}/notes")
    public ResponseEntity<List<CaseNoteResponse>> getCaseNotes(
            @PathVariable Long caseId) {
        return ResponseEntity.ok(caseNoteService.getCaseNotes(caseId));
    }

    /**
     * Retrieves timeline events for a blotter case.
     */
    @GetMapping("/{caseId}/timeline")
    public ResponseEntity<List<TimelineEventResponse>> getCaseTimeline(
            @PathVariable Long caseId) {
        return ResponseEntity.ok(blotterCaseService.getCaseTimeline(caseId));
    }
}
