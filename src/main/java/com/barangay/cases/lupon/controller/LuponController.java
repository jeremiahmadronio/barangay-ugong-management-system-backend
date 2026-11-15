package com.barangay.cases.lupon.controller;

import com.barangay.cases.blotter.dto.SessionFollowUpRequest;
import com.barangay.cases.blotter.dto.SessionFollowUpResponse;
import com.barangay.cases.lupon.dto.*;
import com.barangay.cases.lupon.service.ConciliationService;
import com.barangay.cases.lupon.service.LuponService;
import com.barangay.zshared.audit.annotations.LogAudit;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;
import com.barangay.zshared.mapper.PageResponse;
import com.barangay.zshared.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lupon")
@RequiredArgsConstructor
public class LuponController {

    private final LuponService luponService;
    private final ConciliationService conciliationService;
    private final com.barangay.cases.lupon.service.LuponReports luponReports;
    private final com.barangay.cases.lupon.service.LuponArchive luponArchive;

    /**
     * Retrieves the 4 core Operational Dashboard KPI metrics for Lupong Tagapamayapa.
     * (Total Referred This Week, Under Conciliation, Upcoming Hearings This Week, Total CFA Issued This Week).
     *
     * @return {@link ResponseEntity} of {@link LuponDashboardKpiResponse}
     */
    @GetMapping("/dashboard-kpi")
    public ResponseEntity<LuponDashboardKpiResponse> getDashboardKpis() {
        LuponDashboardKpiResponse response = luponReports.getDashboardKpis();
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves up to 5 recently referred Lupon cases processed today.
     *
     * @return {@link ResponseEntity} of list of {@link com.barangay.cases.blotter.dto.RecentBlotterCaseResponse}
     */
    @GetMapping("/recent-cases-today")
    public ResponseEntity<List<com.barangay.cases.blotter.dto.RecentBlotterCaseResponse>> getRecentCasesToday() {
        List<com.barangay.cases.blotter.dto.RecentBlotterCaseResponse> response = luponReports.getRecentCasesToday();
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves up to 5 of today's scheduled conciliation hearings for Lupon cases.
     *
     * @return {@link ResponseEntity} of list of {@link com.barangay.cases.blotter.dto.QuickMediationSummaryDto}
     */
    @GetMapping("/quick-today-conciliations")
    public ResponseEntity<List<com.barangay.cases.blotter.dto.QuickMediationSummaryDto>> getQuickTodayConciliations() {
        List<com.barangay.cases.blotter.dto.QuickMediationSummaryDto> response = luponReports.getQuickTodayConciliations();
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves analytics KPI metrics for Lupong Tagapamayapa reports within a date range.
     *
     * @param startDate the inclusive starting date (defaults to 1st of month)
     * @param endDate   the inclusive ending date (defaults to today)
     * @return {@link ResponseEntity} of {@link LuponReportKpiResponse}
     */
    @GetMapping("/report-kpi")
    public ResponseEntity<LuponReportKpiResponse> getReportKpi(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate
    ) {
        java.time.LocalDate effectiveEndDate = (endDate != null) ? endDate : java.time.LocalDate.now();
        java.time.LocalDate effectiveStartDate = (startDate != null) ? startDate : effectiveEndDate.withDayOfMonth(1);

        LuponReportKpiResponse report = luponReports.getReportKpi(effectiveStartDate, effectiveEndDate);
        return ResponseEntity.ok(report);
    }

    /**
     * Retrieves monthly conciliation case trend for Lupon reports within a date range.
     *
     * @param startDate inclusive start date
     * @param endDate   inclusive end date
     * @return list of {@link com.barangay.cases.blotter.dto.MonthlyCaseTrendResponse}
     */
    @GetMapping("/reports/monthly-trend")
    public ResponseEntity<List<com.barangay.cases.blotter.dto.MonthlyCaseTrendResponse>> getMonthlyTrend(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate
    ) {
        java.time.LocalDate effectiveEndDate = (endDate != null) ? endDate : java.time.LocalDate.now();
        java.time.LocalDate effectiveStartDate = (startDate != null) ? startDate : effectiveEndDate.minusMonths(5).withDayOfMonth(1);

        List<com.barangay.cases.blotter.dto.MonthlyCaseTrendResponse> list = luponReports.getMonthlyTrendReport(effectiveStartDate, effectiveEndDate);
        return ResponseEntity.ok(list);
    }

    /**
     * Retrieves daily conciliation case trend for Lupon reports within a date range.
     *
     * @param startDate inclusive start date
     * @param endDate   inclusive end date
     * @return list of {@link com.barangay.cases.blotter.dto.DailyCaseTrendResponse}
     */
    @GetMapping("/reports/daily-trend")
    public ResponseEntity<List<com.barangay.cases.blotter.dto.DailyCaseTrendResponse>> getDailyTrend(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate
    ) {
        java.time.LocalDate effectiveEndDate = (endDate != null) ? endDate : java.time.LocalDate.now();
        java.time.LocalDate effectiveStartDate = (startDate != null) ? startDate : effectiveEndDate.minusDays(29);

        List<com.barangay.cases.blotter.dto.DailyCaseTrendResponse> list = luponReports.getDailyTrendReport(effectiveStartDate, effectiveEndDate);
        return ResponseEntity.ok(list);
    }

    /**
     * Retrieves case status breakdown distribution for Lupon reports within a date range.
     *
     * @param startDate inclusive start date
     * @param endDate   inclusive end date
     * @return {@link com.barangay.cases.blotter.dto.CaseStatusBreakdownResponse}
     */
    @GetMapping("/reports/status-breakdown")
    public ResponseEntity<com.barangay.cases.blotter.dto.CaseStatusBreakdownResponse> getStatusBreakdown(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate
    ) {
        java.time.LocalDate effectiveEndDate = (endDate != null) ? endDate : java.time.LocalDate.now();
        java.time.LocalDate effectiveStartDate = (startDate != null) ? startDate : effectiveEndDate.minusMonths(5).withDayOfMonth(1);

        com.barangay.cases.blotter.dto.CaseStatusBreakdownResponse response = luponReports.getStatusBreakdownReport(effectiveStartDate, effectiveEndDate);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves paginated and filtered Lupong Tagapamayapa cases (referred/escalated only).
     *
     * @param filter request containing search, status, natureOfComplaint, page, size
     * @return {@link ResponseEntity} of {@link PageResponse} of {@link LuponTableRowResponse}
     */
    @GetMapping("/table")
    public ResponseEntity<PageResponse<LuponTableRowResponse>> getLuponCases(
            @ModelAttribute LuponFilterRequest filter) {
        PageResponse<LuponTableRowResponse> response = luponService.getLuponCases(filter);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves summary KPI statistics for Lupong Tagapamayapa module.
     *
     * @return {@link ResponseEntity} of {@link LuponStatsResponse}
     */
    @GetMapping("/stats")
    public ResponseEntity<LuponStatsResponse> getLuponStats() {
        LuponStatsResponse response = luponService.getLuponStats();
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves assigned Pangkat Tagapagkasundo panel members for a case.
     *
     * @param caseId the case ID
     * @return list of {@link LuponPanelMemberResponse}
     */
    @GetMapping("/{caseId}/pangkat")
    public ResponseEntity<List<LuponPanelMemberResponse>> getPangkatMembers(
            @PathVariable Long caseId) {
        List<LuponPanelMemberResponse> response = luponService.getPanelMembers(caseId);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists all hearings (Mediation + Conciliation with Pangkat panel attendance) for a given Lupon case.
     *
     * @param caseId the case ID
     * @return ordered list of {@link ConciliationSessionResponse}
     */
    @GetMapping("/{caseId}/conciliation")
    public ResponseEntity<List<ConciliationSessionResponse>> getConciliationSessions(
            @PathVariable Long caseId) {
        List<ConciliationSessionResponse> response = conciliationService.getConciliationSessions(caseId);
        return ResponseEntity.ok(response);
    }

    /**
     * Schedules a new Conciliation Hearing for a Lupon case.
     *
     * @param caseId the case ID
     * @param request payload containing schedule date/time, venue, notes, present panel members
     * @return {@link ConciliationSessionResponse}
     */
    @PostMapping("/{caseId}/conciliation")
    @LogAudit(
            module = AuditModule.LUPON,
            action = "SCHEDULE_CONCILIATION_HEARING",
            severity = Severity.INFO,
            captureNewValue = true
    )
    public ResponseEntity<ConciliationSessionResponse> scheduleConciliation(
            @PathVariable Long caseId,
            @Valid @RequestBody ScheduleConciliationRequest request) {
        if (!SecurityUtils.hasPermission("Manage Conciliation")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to manage conciliation hearings.");
        }
        ConciliationSessionResponse response = conciliationService.scheduleConciliation(caseId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancels a scheduled Conciliation Hearing.
     *
     * @param sessionId the session ID to cancel
     * @return updated {@link ConciliationSessionResponse}
     */
    @PatchMapping("/conciliation/{sessionId}/cancel")
    @LogAudit(
            module = AuditModule.LUPON,
            action = "CANCEL_CONCILIATION_HEARING",
            severity = Severity.WARNING
    )
    public ResponseEntity<ConciliationSessionResponse> cancelConciliation(
            @PathVariable Long sessionId) {
        if (!SecurityUtils.hasPermission("Manage Conciliation")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to cancel conciliation hearings.");
        }
        ConciliationSessionResponse response = conciliationService.cancelConciliation(sessionId);
        return ResponseEntity.ok(response);
    }

    /**
     * Records or updates minutes and Pangkat attendance for a Conciliation session.
     *
     * @param sessionId the session ID
     * @param request payload containing minutes and panel attendance
     * @return updated {@link ConciliationSessionResponse}
     */
    @PostMapping("/conciliation/{sessionId}/minutes")
    @LogAudit(
            module = AuditModule.LUPON,
            action = "RECORD_CONCILIATION_MINUTES",
            severity = Severity.INFO,
            captureNewValue = true
    )
    public ResponseEntity<ConciliationSessionResponse> recordConciliationMinutes(
            @PathVariable Long sessionId,
            @RequestBody RecordConciliationMinutesRequest request) {
        if (!SecurityUtils.hasPermission("Manage Conciliation")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to record conciliation minutes.");
        }
        ConciliationSessionResponse response = conciliationService.recordConciliationMinutes(sessionId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Appends a follow-up remark to a Conciliation hearing session.
     *
     * @param sessionId the conciliation session ID
     * @param request   the follow-up payload containing remarks
     * @return {@link ResponseEntity} of {@link SessionFollowUpResponse}
     */
    @PostMapping("/conciliation/{sessionId}/followup")
    @LogAudit(
            module = AuditModule.LUPON,
            action = "ADD_CONCILIATION_FOLLOWUP",
            severity = Severity.INFO,
            captureNewValue = true
    )
    public ResponseEntity<SessionFollowUpResponse> addSessionFollowUp(
            @PathVariable Long sessionId,
            @Valid @RequestBody SessionFollowUpRequest request) {
        if (!SecurityUtils.hasPermission("Manage Conciliation")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to manage conciliation proceedings.");
        }
        SessionFollowUpResponse response = conciliationService.addConciliationFollowUp(sessionId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all follow-up remarks for a Conciliation hearing session.
     *
     * @param sessionId the conciliation session ID
     * @return {@link ResponseEntity} of list of {@link SessionFollowUpResponse}
     */
    @GetMapping("/conciliation/{sessionId}/followup")
    public ResponseEntity<List<SessionFollowUpResponse>> getSessionFollowUps(
            @PathVariable Long sessionId) {
        List<SessionFollowUpResponse> response = conciliationService.getConciliationFollowUps(sessionId);
        return ResponseEntity.ok(response);
    }

    /**
     * Extends the Lupon conciliation period by 15 days (one-time allowable extension).
     *
     * @param caseId  the case ID
     * @param request the extension request
     * @return {@link ResponseEntity} of {@link com.barangay.cases.blotter.dto.BlotterCaseDetailResponse}
     */
    @PostMapping("/{caseId}/extend")
    @LogAudit(
            module = AuditModule.LUPON,
            action = "EXTEND_CONCILIATION_PERIOD",
            severity = Severity.INFO,
            captureNewValue = true
    )
    public ResponseEntity<com.barangay.cases.blotter.dto.BlotterCaseDetailResponse> extendLuponCase(
            @PathVariable Long caseId,
            @Valid @RequestBody com.barangay.cases.blotter.dto.ExtendCaseRequest request) {
        if (!SecurityUtils.hasPermission("Manage Conciliation")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to extend conciliation period.");
        }
        com.barangay.cases.blotter.dto.BlotterCaseDetailResponse response = luponService.extendLuponCase(caseId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves summary KPI metrics for Lupon Archive and Trash Vault.
     *
     * @return {@link ResponseEntity} of {@link com.barangay.cases.blotter.dto.BlotterLifecycleKpiResponse}
     */
    @GetMapping("/archive-kpi")
    public ResponseEntity<com.barangay.cases.blotter.dto.BlotterLifecycleKpiResponse> getLuponLifecycleKpi() {
        com.barangay.cases.blotter.dto.BlotterLifecycleKpiResponse response = luponArchive.getLuponLifecycleKpi();
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves paginated archived Lupon cases for the Archive Table.
     *
     * @param filter filter request
     * @return {@link PageResponse} of {@link com.barangay.cases.blotter.dto.BlotterArchiveItemResponse}
     */
    @GetMapping("/archive-table")
    public ResponseEntity<PageResponse<com.barangay.cases.blotter.dto.BlotterArchiveItemResponse>> getArchivedLuponCases(
            @ModelAttribute com.barangay.cases.blotter.dto.BlotterArchiveFilterRequest filter) {
        PageResponse<com.barangay.cases.blotter.dto.BlotterArchiveItemResponse> response = luponArchive.getArchivedLuponCases(filter);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves paginated soft-deleted Lupon cases for the Recycle Bin / Trash Table.
     *
     * @param filter filter request
     * @return {@link PageResponse} of {@link com.barangay.cases.blotter.dto.BlotterRecentDeleteItemResponse}
     */
    @GetMapping("/recent-delete-table")
    public ResponseEntity<PageResponse<com.barangay.cases.blotter.dto.BlotterRecentDeleteItemResponse>> getRecentDeletedLuponCases(
            @ModelAttribute com.barangay.cases.blotter.dto.BlotterArchiveFilterRequest filter) {
        PageResponse<com.barangay.cases.blotter.dto.BlotterRecentDeleteItemResponse> response = luponArchive.getRecentDeletedLuponCases(filter);
        return ResponseEntity.ok(response);
    }

    /**
     * Archives a resolved or completed Lupon case.
     *
     * @param caseId  the case ID
     * @param request archive remarks payload
     */
    @PatchMapping("/archive-case/{caseId}")
    @LogAudit(module = AuditModule.LUPON, action = "ARCHIVE_LUPON_CASE", severity = Severity.INFO,
            captureOldValue = true, oldValueArgIndex = 1, captureNewValue = false)
    public ResponseEntity<Void> archiveCase(
            @PathVariable Long caseId,
            @Valid @RequestBody com.barangay.cases.blotter.dto.ArchiveCaseRequest request) {
        if (!SecurityUtils.hasPermission("Archive Case")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to archive cases.");
        }
        luponArchive.archiveCase(caseId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores an archived Lupon case back to active records.
     *
     * @param caseId the case ID
     */
    @PatchMapping("/{caseId}/restore-archive")
    @LogAudit(module = AuditModule.LUPON, action = "RESTORE_LUPON_CASE", severity = Severity.INFO,
            captureOldValue = true, oldValueArgIndex = 0, captureNewValue = false)
    public ResponseEntity<Void> restoreArchivedCase(@PathVariable Long caseId) {
        if (!SecurityUtils.hasPermission("Restore Case")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to restore cases.");
        }
        luponArchive.restoreArchivedCase(caseId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Moves a Lupon case to the recycle bin (soft delete).
     *
     * @param caseId the case ID
     */
    @PatchMapping("/{caseId}/soft-delete")
    @LogAudit(module = AuditModule.LUPON, action = "SOFT_DELETE_LUPON_CASE", severity = Severity.WARNING,
            captureOldValue = true, oldValueArgIndex = 0, captureNewValue = false)
    public ResponseEntity<Void> softDeleteCase(@PathVariable Long caseId) {
        if (!SecurityUtils.hasPermission("Delete Case")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to delete cases.");
        }
        luponArchive.softDelete(caseId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a soft-deleted Lupon case from recycle bin back to active records.
     *
     * @param caseId the case ID
     */
    @PatchMapping("/{caseId}/restore-trash")
    @LogAudit(module = AuditModule.LUPON, action = "RESTORE_LUPON_CASE_FROM_TRASH", severity = Severity.INFO,
            captureOldValue = true, oldValueArgIndex = 0, captureNewValue = false)
    public ResponseEntity<Void> restoreDeletedCase(@PathVariable Long caseId) {
        if (!SecurityUtils.hasPermission("Restore Case")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to restore cases.");
        }
        luponArchive.restoreArchivedCase(caseId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Permanently purges a Lupon case from trash (audit-safe flag).
     *
     * @param caseId the case ID
     */
    @DeleteMapping("/{caseId}/permanent-delete")
    @LogAudit(module = AuditModule.LUPON, action = "PERMANENT_DELETE_LUPON_CASE", severity = Severity.CRITICAL,
            captureOldValue = true, oldValueArgIndex = 0, captureNewValue = false)
    public ResponseEntity<Void> permanentDeleteCase(@PathVariable Long caseId) {
        if (!SecurityUtils.hasPermission("Permanent Delete Case")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to permanently delete cases.");
        }
        luponArchive.permanentDeleteCaseByFlag(caseId);
        return ResponseEntity.noContent().build();
    }
}
