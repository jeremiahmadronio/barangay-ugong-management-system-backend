package com.barangay.cases.blotter.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.barangay.cases.blotter.dto.*;
import com.barangay.cases.blotter.service.*;
import com.barangay.cases.lupon.dto.LuponReferRalRequest;
import com.barangay.cases.lupon.dto.LuponReferralResponse;
import com.barangay.cases.lupon.service.LuponService;
import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.barangay.cases.shared.service.CaseNoteService;
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
    private final BlotterReportService blotterReportService;
    private final BlotterArchiveAndDeleteService archiveAndDeleteService;
    private final LuponService luponService;

    /**
     * Retrieves paginated and filtered blotter cases.
     * Uses server-side pagination without findAll().
     *
     * @param filter request containing search, status, filingType, natureOfComplaint, page, size
     * @return {@link PageResponse} containing the paginated list of blotter cases
     */
    @GetMapping
    public ResponseEntity<PageResponse<BlotterTableRowResponse>> getBlotterCases(
            @ModelAttribute BlotterFilterRequest filter) {
        PageResponse<BlotterTableRowResponse> response = blotterCaseService.getBlotterCases(filter);
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

    @PutMapping("/{caseId}")
    @LogAudit(module = AuditModule.BLOTTER, action = "UPDATE_BLOTTER_CASE", severity = Severity.INFO)
    public ResponseEntity<BlotterCaseDetailResponse> updateBlotterCase(
            @PathVariable Long caseId,
            @Valid @RequestBody BlotterCaseUpdateRequest request
    ) {
        BlotterCaseDetailResponse response = blotterCaseService.updateBlotterCase(caseId, request);
        return ResponseEntity.ok(response);
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

    /**
     * Updates the status of a blotter case (e.g. DISMISSED, SETTLED) with optional status remarks.
     *
     * @param id the blotter case ID
     * @param request the status update request payload containing status and remarks
     * @return updated {@link BlotterCaseDetailResponse}
     */
    @PatchMapping("/{id}/status")
    @LogAudit(module = AuditModule.BLOTTER, action = "UPDATE_CASE_STATUS", severity = Severity.INFO)
    public ResponseEntity<BlotterCaseDetailResponse> updateCaseStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCaseStatusRequest request) {
        return ResponseEntity.ok(blotterCaseService.updateCaseStatus(id, request));
    }

    /**
     * Reopens a closed, settled, or dismissed blotter case by creating a new case cycle.
     *
     * @param id      the ID of the closed or dismissed case
     * @param request the reopen request payload containing remarks
     * @return newly created reopened {@link BlotterCaseDetailResponse}
     */
    @PostMapping("/{id}/reopen")
    @LogAudit(module = AuditModule.BLOTTER, action = "REOPEN_CASE", severity = Severity.INFO)
    public ResponseEntity<BlotterCaseDetailResponse> reopenCase(
            @PathVariable Long id,
            @Valid @RequestBody ReopenCaseRequest request) {
        BlotterCaseDetailResponse response = blotterCaseService.reopenCase(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }



    @GetMapping("/monthly-trend")
    public ResponseEntity<List<MonthlyCaseTrendResponse>> getMonthlyTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long departmentId
    ) {
        LocalDate effectiveEndDate = (endDate != null) ? endDate : LocalDate.now();
        LocalDate effectiveStartDate = (startDate != null)
                ? startDate
                : effectiveEndDate.minusMonths(5).withDayOfMonth(1);

        List<MonthlyCaseTrendResponse> report = blotterReportService.getMonthlyTrendReport(
                effectiveStartDate,
                effectiveEndDate,
                departmentId
        );

        return ResponseEntity.ok(report);
    }

    @GetMapping("/daily-trend")
    public ResponseEntity<List<DailyCaseTrendResponse>> getDailyTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long departmentId
    ) {
        LocalDate effectiveEndDate = (endDate != null) ? endDate : LocalDate.now();
        LocalDate effectiveStartDate = (startDate != null)
                ? startDate
                : effectiveEndDate.withDayOfMonth(1);

        List<DailyCaseTrendResponse> report = blotterReportService.getDailyTrendReport(
                effectiveStartDate,
                effectiveEndDate,
                departmentId
        );

        return ResponseEntity.ok(report);
    }



    /**
     * Retrieves the settlement efficiency rate and breakdown for a specified department and date range.
     * Defaults to the current month to date if no date range is provided.
     *
     * @param startDate    optional start date (defaults to 1st day of current month)
     * @param endDate      optional end date (defaults to today)
     * @param departmentId optional department filter (e.g., Blotter Department ID)
     * @return response entity containing {@link SettlementEfficiencyResponse}
     */
    @GetMapping("/settlement-efficiency")
    public ResponseEntity<SettlementEfficiencyResponse> getSettlementEfficiency(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long departmentId
    ) {
        LocalDate effectiveEndDate = (endDate != null) ? endDate : LocalDate.now();
        LocalDate effectiveStartDate = (startDate != null) ? startDate : effectiveEndDate.withDayOfMonth(1);

        SettlementEfficiencyResponse report = blotterReportService.getSettlementEfficiency(
                effectiveStartDate,
                effectiveEndDate,
                departmentId
        );

        return ResponseEntity.ok(report);
    }


    @GetMapping("/status-breakdown")
    public ResponseEntity<CaseStatusBreakdownResponse> getStatusBreakdown(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long departmentId
    ) {
        LocalDate effectiveEndDate = (endDate != null) ? endDate : LocalDate.now();
        LocalDate effectiveStartDate = (startDate != null) ? startDate : effectiveEndDate.withDayOfMonth(1);

        CaseStatusBreakdownResponse report = blotterReportService.getStatusBreakdownReport(
                effectiveStartDate,
                effectiveEndDate,
                departmentId
        );

        return ResponseEntity.ok(report);
    }


    /**
     * Endpoint retrieving the settlement and referral distribution for formal complaints.
     * Defaults to the first day of the current month up to the current date if parameters are omitted.
     *
     * @param startDate    optional start date (format: YYYY-MM-DD)
     * @param endDate      optional end date (format: YYYY-MM-DD)
     * @param departmentId optional department ID filter (e.g., Blotter Department)
     * @return {@link ResponseEntity} containing {@link FormalComplaintReportResponse}
     */
    @GetMapping("/formal-complaints/resolution-efficiency")
    public ResponseEntity<FormalComplaintReportResponse> getFormalComplaintResolutionReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long departmentId
    ) {
        LocalDate effectiveEndDate = (endDate != null) ? endDate : LocalDate.now();
        LocalDate effectiveStartDate = (startDate != null) ? startDate : effectiveEndDate.withDayOfMonth(1);

        FormalComplaintReportResponse report = blotterReportService.getResolutionReport(
                effectiveStartDate,
                effectiveEndDate,
                departmentId
        );

        return ResponseEntity.ok(report);
    }

    /**
     * Endpoint retrieving date-filtered KPI statistics for Blotter Department reports.
     *
     * @param startDate    optional start date (defaults to 1st day of current month)
     * @param endDate      optional end date (defaults to today)
     * @param departmentId optional department filter
     * @return {@link BlotterReportKpiResponse}
     */
    @GetMapping("/report-kpi")
    public ResponseEntity<BlotterReportKpiResponse> getReportKpi(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long departmentId
    ) {
        LocalDate effectiveEndDate = (endDate != null) ? endDate : LocalDate.now();
        LocalDate effectiveStartDate = (startDate != null) ? startDate : effectiveEndDate.withDayOfMonth(1);

        BlotterReportKpiResponse report = blotterReportService.getReportKpi(
                effectiveStartDate,
                effectiveEndDate,
                departmentId
        );

        return ResponseEntity.ok(report);
    }



    /**
     * Endpoint retrieving lifecycle and trash statistics exclusively for the Blotter Department.
     *
     * @return {@link BlotterLifecycleKpiResponse}
     */
    @GetMapping("/archive-kpi")
    public ResponseEntity<BlotterLifecycleKpiResponse> getBlotterLifecycleKpi() {
        BlotterLifecycleKpiResponse response = archiveAndDeleteService.getBlotterLifecycleKpi();
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint retrieving paginated archived blotter cases for the Archive Table.
     *
     * @param filter {@link BlotterArchiveFilterRequest} search and filingType criteria
     * @return {@link PageResponse} of {@link BlotterArchiveItemResponse}
     */
    @GetMapping("/archive-table")
    public ResponseEntity<PageResponse<BlotterArchiveItemResponse>> getArchivedBlotterCases(
            @ModelAttribute BlotterArchiveFilterRequest filter) {
        PageResponse<BlotterArchiveItemResponse> response = archiveAndDeleteService.getArchivedBlotterCases(filter);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint retrieving paginated soft-deleted blotter cases for the Recent Delete / Trash Table.
     * Excludes permanently deleted records.
     *
     * @param filter {@link BlotterArchiveFilterRequest} search and filingType criteria
     * @return {@link PageResponse} of {@link BlotterRecentDeleteItemResponse}
     */
    @GetMapping("/recent-delete")
    public ResponseEntity<PageResponse<BlotterRecentDeleteItemResponse>> getRecentDeletedBlotterCases(
            @ModelAttribute BlotterArchiveFilterRequest filter) {
        PageResponse<BlotterRecentDeleteItemResponse> response = archiveAndDeleteService.getRecentDeletedBlotterCases(filter);
        return ResponseEntity.ok(response);
    }


    /**
     * Archives a resolved or closed blotter case.
     *
     * @param caseId  target case ID
     * @param request archive remarks payload
     * @return success message
     */
    @PatchMapping("/archive-case/{caseId}")
    public ResponseEntity<Void> archiveCase(
          @PathVariable Long caseId,
           @Valid @RequestBody ArchiveCaseRequest request
    ){
        archiveAndDeleteService.archiveCase(caseId, request);
        return ResponseEntity.ok().build();
    }


    /**
     * Restores an archived case back to the active records table.
     *
     * @param caseId target case ID
     * @return success message
     */
    @PatchMapping("/{caseId}/restore-archive")
    public ResponseEntity<Void> restoreArchivedCase(
            @PathVariable Long caseId
    ) {
        archiveAndDeleteService.restoreArchivedCase(caseId);
        return ResponseEntity.ok().build();

    }


    @PatchMapping("/{caseId}/soft-delete")
    public ResponseEntity<Map<String, String>> softDelete(@PathVariable Long caseId) {
        archiveAndDeleteService.softDelete(caseId);
        return ResponseEntity.ok(Map.of("message", "Case moved to trash successfully"));
    }

    @DeleteMapping("/{caseId}/permanent-delete")
    public ResponseEntity<Map<String, String>> permanentDelete(@PathVariable Long caseId) {
        archiveAndDeleteService.permanentDeleteCaseByFlag(caseId);
        return ResponseEntity.ok(Map.of("message", "Case permanently deleted"));
    }

    /**
     * Endpoint retrieving the 4 key operational dashboard metrics for Blotter.
     *
     * @return {@link BlotterDashboardKpiResponse}
     */
    @GetMapping("/dashboard-kpi")
    public ResponseEntity<BlotterDashboardKpiResponse> getDashboardKpis() {
        BlotterDashboardKpiResponse response = blotterReportService.getDashboardKpis();
        return ResponseEntity.ok(response);
    }


    /**
     * Endpoint retrieving the top 5 most recent blotter cases issued today for the dashboard.
     *
     * @return {@link ResponseEntity} containing a list of {@link RecentBlotterCaseResponse}
     */
    @GetMapping("/recent-today")
    public ResponseEntity<List<RecentBlotterCaseResponse>> getRecentCasesToday() {
        List<RecentBlotterCaseResponse> response = blotterReportService.getRecentCasesToday();
        return ResponseEntity.ok(response);
    }


    /**
     * Endpoint para sa dashboard quick mediation widget (Top 5 scheduled hearings today).
     *
     * @return List ng {@link QuickMediationSummaryDto}
     */
    @GetMapping("/mediations-today-summary")
    public ResponseEntity<List<QuickMediationSummaryDto>> getQuickTodayMediations() {
        List<QuickMediationSummaryDto> response = blotterReportService.getQuickTodayMediations();
        return ResponseEntity.ok(response);
    }


    @PostMapping("/refer/{caseNumber}")
    public ResponseEntity<LuponReferralResponse> referToLupon(
            @PathVariable String caseNumber,
            @Valid @RequestBody LuponReferRalRequest request
    ) {
        LuponReferralResponse response = luponService.processLuponReferral(caseNumber, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Extends the Blotter mediation period by 15 days (one-time allowable extension).
     *
     * @param caseId  the case ID
     * @param request the extension request
     * @return {@link ResponseEntity} of {@link BlotterCaseDetailResponse}
     */
    @PostMapping("/{caseId}/extend")
    @LogAudit(
            module = AuditModule.BLOTTER,
            action = "EXTEND_MEDIATION_PERIOD",
            severity = Severity.INFO,
            captureNewValue = true
    )
    public ResponseEntity<BlotterCaseDetailResponse> extendCase(
            @PathVariable Long caseId,
            @Valid @RequestBody ExtendCaseRequest request) {
        BlotterCaseDetailResponse response = blotterCaseService.extendCase(caseId, request);
        return ResponseEntity.ok(response);
    }
}
