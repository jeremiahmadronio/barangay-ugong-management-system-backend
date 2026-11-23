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
import com.barangay.zshared.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;

import lombok.RequiredArgsConstructor;

/**
 * REST Controller for Blotter Case Management.
 */
@RestController
@RequestMapping("/api/v1/blotter")
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
     */
    @GetMapping
    public ResponseEntity<PageResponse<BlotterTableRowResponse>> getBlotterCases(
            @ModelAttribute BlotterFilterRequest filter) {
        PageResponse<BlotterTableRowResponse> response = blotterCaseService.getBlotterCases(filter);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves KPI Statistics for Blotter cases with month-to-date vs full previous month trends.
     */
    @GetMapping("/stats")
    public ResponseEntity<BlotterStatsResponse> getBlotterStats() {
        BlotterStatsResponse stats = blotterCaseService.getBlotterStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Retrieves a single blotter case detail by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BlotterCaseDetailResponse> getBlotterCaseById(@PathVariable Long id) {
        BlotterCaseDetailResponse response = blotterCaseService.getBlotterCaseById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new Blotter Case entry (Formal Complaint or For the Record).
     */
    @PostMapping
    @LogAudit(module = AuditModule.BLOTTER, action = "CREATE_BLOTTER_CASE", severity = Severity.INFO)
    public ResponseEntity<BlotterCaseDetailResponse> createBlotterCase(@Valid @RequestBody BlotterCaseCreateRequest request) {
        if (!SecurityUtils.hasPermission("File Complaint")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to file complaints.");
        }
        BlotterCaseDetailResponse response = blotterCaseService.createBlotterCase(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{caseId}")
    @LogAudit(module = AuditModule.BLOTTER, action = "UPDATE_BLOTTER_CASE", severity = Severity.INFO)
    public ResponseEntity<BlotterCaseDetailResponse> updateBlotterCase(
            @PathVariable Long caseId,
            @Valid @RequestBody BlotterCaseUpdateRequest request
    ) {
        if (!SecurityUtils.hasPermission("Edit Case")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to edit cases.");
        }
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
     */
    @GetMapping("/{caseId}/mediation")
    public ResponseEntity<List<MediationSessionResponse>> getMediationSessions(
            @PathVariable Long caseId) {
        return ResponseEntity.ok(mediationSessionService.getMediationSessions(caseId));
    }

    /**
     * Schedules a new mediation hearing for a given blotter case.
     */
    @PostMapping("/{caseId}/mediation")
    @LogAudit(module = AuditModule.BLOTTER, action = "SCHEDULE_MEDIATION_HEARING", severity = Severity.INFO,
            captureOldValue = true, oldValueArgIndex = 1, captureNewValue = true)
    public ResponseEntity<MediationSessionResponse> scheduleMediation(
            @PathVariable Long caseId,
            @Valid @RequestBody MediationSessionRequest request) {
        if (!SecurityUtils.hasPermission("Manage Mediation")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to schedule mediation hearings.");
        }
        MediationSessionResponse response = mediationSessionService.scheduleMediation(caseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Cancels a scheduled mediation session.
     */
    @PatchMapping("/mediation/{sessionId}/cancel")
    @LogAudit(module = AuditModule.BLOTTER, action = "CANCEL_MEDIATION_HEARING", severity = Severity.WARNING,
            captureOldValue = true, oldValueArgIndex = 0)
    public ResponseEntity<MediationSessionResponse> cancelMediation(
            @PathVariable Long sessionId) {
        if (!SecurityUtils.hasPermission("Manage Mediation")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to cancel mediation hearings.");
        }
        return ResponseEntity.ok(mediationSessionService.cancelMediation(sessionId));
    }

    /**
     * Records or updates minutes for a mediation session.
     */
    @PostMapping("/mediation/{sessionId}/minutes")
    @LogAudit(module = AuditModule.BLOTTER, action = "RECORD_MEDIATION_MINUTES", severity = Severity.INFO,
            captureOldValue = true, oldValueArgIndex = 1, captureNewValue = true)
    public ResponseEntity<MediationSessionResponse> recordSessionMinutes(
            @PathVariable Long sessionId,
            @Valid @RequestBody SessionMinutesRequest request) {
        if (!SecurityUtils.hasPermission("Manage Mediation")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to record mediation minutes.");
        }
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
    @LogAudit(module = AuditModule.BLOTTER, action = "ADD_MEDIATION_FOLLOWUP", severity = Severity.INFO,
            captureOldValue = true, oldValueArgIndex = 1, captureNewValue = true)
    public ResponseEntity<SessionFollowUpResponse> addSessionFollowUp(
            @PathVariable Long sessionId,
            @Valid @RequestBody SessionFollowUpRequest request) {
        if (!SecurityUtils.hasPermission("Manage Mediation")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to add mediation follow-ups.");
        }
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
    @LogAudit(module = AuditModule.BLOTTER, action = "ADD_BLOTTER_CASE_NOTE", severity = Severity.INFO,
            captureOldValue = true, oldValueArgIndex = 1, captureNewValue = true)
    public ResponseEntity<CaseNoteResponse> addCaseNote(
            @PathVariable Long caseId,
            @Valid @RequestBody CaseNoteRequest request) {
        if (!SecurityUtils.hasPermission("Manage Notes")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to add case notes.");
        }
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
     */
    @PatchMapping("/{id}/status")
    @LogAudit(module = AuditModule.BLOTTER, action = "UPDATE_CASE_STATUS", severity = Severity.INFO)
    public ResponseEntity<BlotterCaseDetailResponse> updateCaseStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCaseStatusRequest request) {
        if (request != null && request.getStatus() != null) {
            String targetStatus = String.valueOf(request.getStatus());
            if (("SETTLED".equalsIgnoreCase(targetStatus) || "RESOLVED".equalsIgnoreCase(targetStatus))
                    && !SecurityUtils.hasPermission("Settled Case")) {
                throw new AccessDeniedException("Access Denied: You do not have permission to settle cases.");
            }
            if (("DISMISSED".equalsIgnoreCase(targetStatus) || "WITHDRAWN".equalsIgnoreCase(targetStatus))
                    && !SecurityUtils.hasPermission("Dismiss Case")) {
                throw new AccessDeniedException("Access Denied: You do not have permission to dismiss cases.");
            }
            if (("CERTIFIED_TO_FILE_ACTION".equalsIgnoreCase(targetStatus) || "CFA_ISSUED".equalsIgnoreCase(targetStatus))
                    && !SecurityUtils.hasPermission("Issue Certified to File Action (CFA)")) {
                throw new AccessDeniedException("Access Denied: You do not have permission to issue CFA.");
            }
        }
        return ResponseEntity.ok(blotterCaseService.updateCaseStatus(id, request));
    }

    /**
     * Reopens a closed, settled, or dismissed blotter case by creating a new case cycle.
     */
    @PostMapping("/{id}/reopen")
    @LogAudit(module = AuditModule.BLOTTER, action = "REOPEN_CASE", severity = Severity.INFO)
    public ResponseEntity<BlotterCaseDetailResponse> reopenCase(
            @PathVariable Long id,
            @Valid @RequestBody ReopenCaseRequest request) {
        if (!SecurityUtils.hasPermission("Re-open Case")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to reopen cases.");
        }
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
     */
    @GetMapping("/archive-kpi")
    public ResponseEntity<BlotterLifecycleKpiResponse> getBlotterLifecycleKpi() {
        BlotterLifecycleKpiResponse response = archiveAndDeleteService.getBlotterLifecycleKpi();
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint retrieving paginated archived blotter cases for the Archive Table.
     */
    @GetMapping("/archive-table")
    public ResponseEntity<PageResponse<BlotterArchiveItemResponse>> getArchivedBlotterCases(
            @ModelAttribute BlotterArchiveFilterRequest filter) {
        PageResponse<BlotterArchiveItemResponse> response = archiveAndDeleteService.getArchivedBlotterCases(filter);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint retrieving paginated soft-deleted blotter cases for the Recent Delete / Trash Table.
     */
    @GetMapping("/recent-delete")
    public ResponseEntity<PageResponse<BlotterRecentDeleteItemResponse>> getRecentDeletedBlotterCases(
            @ModelAttribute BlotterArchiveFilterRequest filter) {
        PageResponse<BlotterRecentDeleteItemResponse> response = archiveAndDeleteService.getRecentDeletedBlotterCases(filter);
        return ResponseEntity.ok(response);
    }


    /**
     * Archives a resolved or closed blotter case.
     */
    @PatchMapping("/archive-case/{caseId}")
    @LogAudit(module = AuditModule.BLOTTER, action = "ARCHIVE_BLOTTER_CASE", severity = Severity.INFO,
            captureOldValue = true, oldValueArgIndex = 1, captureNewValue = false)
    public ResponseEntity<Void> archiveCase(
          @PathVariable Long caseId,
           @Valid @RequestBody ArchiveCaseRequest request
    ){
        if (!SecurityUtils.hasPermission("Archive Case")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to archive cases.");
        }
        archiveAndDeleteService.archiveCase(caseId, request);
        return ResponseEntity.ok().build();
    }


    /**
     * Restores an archived case back to the active records table.
     */
    @PatchMapping("/{caseId}/restore-archive")
    @LogAudit(module = AuditModule.BLOTTER, action = "RESTORE_BLOTTER_CASE", severity = Severity.INFO,
            captureOldValue = true, oldValueArgIndex = 0, captureNewValue = false)
    public ResponseEntity<Void> restoreArchivedCase(
            @PathVariable Long caseId
    ) {
        if (!SecurityUtils.hasPermission("Restore Case")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to restore cases.");
        }
        archiveAndDeleteService.restoreArchivedCase(caseId);
        return ResponseEntity.ok().build();

    }


    @PatchMapping("/{caseId}/soft-delete")
    @LogAudit(module = AuditModule.BLOTTER, action = "SOFT_DELETE_BLOTTER_CASE", severity = Severity.WARNING,
            captureOldValue = true, oldValueArgIndex = 0)
    public ResponseEntity<Map<String, String>> softDelete(@PathVariable Long caseId) {
        if (!SecurityUtils.hasPermission("Delete Case")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to delete cases.");
        }
        archiveAndDeleteService.softDelete(caseId);
        return ResponseEntity.ok(Map.of("message", "Case moved to trash successfully"));
    }

    @DeleteMapping("/{caseId}/permanent-delete")
    @LogAudit(module = AuditModule.BLOTTER, action = "PERMANENT_DELETE_BLOTTER_CASE", severity = Severity.CRITICAL,
            captureOldValue = true, oldValueArgIndex = 0)
    public ResponseEntity<Map<String, String>> permanentDelete(@PathVariable Long caseId) {
        if (!SecurityUtils.hasPermission("Permanent Delete Case")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to permanently delete cases.");
        }
        archiveAndDeleteService.permanentDeleteCaseByFlag(caseId);
        return ResponseEntity.ok(Map.of("message", "Case permanently deleted"));
    }

    /**
     * Endpoint retrieving the 4 key operational dashboard metrics for Blotter.
     */
    @GetMapping("/dashboard-kpi")
    public ResponseEntity<BlotterDashboardKpiResponse> getDashboardKpis() {
        BlotterDashboardKpiResponse response = blotterReportService.getDashboardKpis();
        return ResponseEntity.ok(response);
    }


    /**
     * Endpoint retrieving the top 5 most recent blotter cases issued today for the dashboard.
     */
    @GetMapping("/recent-today")
    public ResponseEntity<List<RecentBlotterCaseResponse>> getRecentCasesToday() {
        List<RecentBlotterCaseResponse> response = blotterReportService.getRecentCasesToday();
        return ResponseEntity.ok(response);
    }


    /**
     * Endpoint para sa dashboard quick mediation widget (Top 5 scheduled hearings today).
     */
    @GetMapping("/mediations-today-summary")
    public ResponseEntity<List<QuickMediationSummaryDto>> getQuickTodayMediations() {
        List<QuickMediationSummaryDto> response = blotterReportService.getQuickTodayMediations();
        return ResponseEntity.ok(response);
    }


    @PostMapping("/refer/{caseNumber}")
    @LogAudit(module = AuditModule.BLOTTER, action = "REFER_CASE_TO_LUPON", severity = Severity.INFO,
            captureOldValue = true, oldValueArgIndex = 1, captureNewValue = true)
    public ResponseEntity<LuponReferralResponse> referToLupon(
            @PathVariable String caseNumber,
            @Valid @RequestBody LuponReferRalRequest request
    ) {
        if (!SecurityUtils.hasPermission("Escalate to Lupon")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to refer cases to Lupon.");
        }
        LuponReferralResponse response = luponService.processLuponReferral(caseNumber, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Extends the Blotter mediation period by 15 days (one-time allowable extension).
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

    /**
     * Searches eligible issued FTJS certificates for Affidavit of Loss linkage.
     */
    @GetMapping("/search-ftjs")
    public ResponseEntity<List<FtjsSearchDTO>> searchFtjsApplications(
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(blotterCaseService.searchFtjsApplications(query));
    }

    /**
     * Verifies or rejects document reissuance clearance for a Blotter case.
     */
    @PostMapping("/{caseId}/verify-document-reissuance")
    @LogAudit(module = AuditModule.BLOTTER, action = "VERIFY_DOCUMENT_REISSUANCE", severity = Severity.INFO)
    public ResponseEntity<String> verifyDocumentReissuance(
            @PathVariable Long caseId,
            @Valid @RequestBody VerifyDocumentReissuanceDTO dto) {
        blotterCaseService.verifyDocumentReissuance(caseId, dto);
        return ResponseEntity.ok("Document reissuance status updated successfully.");
    }
}
