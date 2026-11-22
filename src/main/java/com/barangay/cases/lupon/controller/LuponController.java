package com.barangay.cases.lupon.controller;

import com.barangay.cases.blotter.dto.*;
import com.barangay.cases.lupon.dto.*;
import com.barangay.cases.lupon.service.ConciliationService;
import com.barangay.cases.lupon.service.LuponArchive;
import com.barangay.cases.lupon.service.LuponReports;
import com.barangay.cases.lupon.service.LuponService;
import com.barangay.zshared.audit.annotations.LogAudit;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;
import com.barangay.zshared.mapper.PageResponse;
import com.barangay.zshared.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for Lupong Tagapamayapa operations, conciliation hearings, and reports.
 */
@RestController
@RequestMapping("/api/v1/lupon")
@RequiredArgsConstructor
public class LuponController {

    private final LuponService luponService;
    private final ConciliationService conciliationService;
    private final LuponReports luponReports;
    private final LuponArchive luponArchive;

        /**
     * Retrieves the 4 core Operational Dashboard KPI metrics for Lupong Tagapamayapa. (Total Referred This Week, Under Conciliation, Upcoming Hearings This Week, Total CFA Issued This Week).
     */
    @GetMapping("/dashboard-kpi")
    public ResponseEntity<LuponDashboardKpiResponse> getDashboardKpis() {
        LuponDashboardKpiResponse response = luponReports.getDashboardKpis();
        return ResponseEntity.ok(response);
    }

        /**
     * Retrieves up to 5 recently referred Lupon cases processed today.
     */
    @GetMapping("/recent-cases-today")
    public ResponseEntity<List<RecentBlotterCaseResponse>> getRecentCasesToday() {
        List<RecentBlotterCaseResponse> response = luponReports.getRecentCasesToday();
        return ResponseEntity.ok(response);
    }

        /**
     * Retrieves up to 5 of today's scheduled conciliation hearings for Lupon cases.
     */
    @GetMapping("/quick-today-conciliations")
    public ResponseEntity<List<QuickMediationSummaryDto>> getQuickTodayConciliations() {
        List<QuickMediationSummaryDto> response = luponReports.getQuickTodayConciliations();
        return ResponseEntity.ok(response);
    }

        /**
     * Retrieves analytics KPI metrics for Lupong Tagapamayapa reports within a date range.
     */
    @GetMapping("/report-kpi")
    public ResponseEntity<LuponReportKpiResponse> getReportKpi(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDate effectiveEndDate = (endDate != null) ? endDate : LocalDate.now();
        LocalDate effectiveStartDate = (startDate != null) ? startDate : effectiveEndDate.withDayOfMonth(1);

        LuponReportKpiResponse report = luponReports.getReportKpi(effectiveStartDate, effectiveEndDate);
        return ResponseEntity.ok(report);
    }

        /**
     * Retrieves monthly conciliation case trend for Lupon reports within a date range.
     */
    @GetMapping("/reports/monthly-trend")
    public ResponseEntity<List<MonthlyCaseTrendResponse>> getMonthlyTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDate effectiveEndDate = (endDate != null) ? endDate : LocalDate.now();
        LocalDate effectiveStartDate = (startDate != null) ? startDate : effectiveEndDate.minusMonths(5).withDayOfMonth(1);

        List<MonthlyCaseTrendResponse> list = luponReports.getMonthlyTrendReport(effectiveStartDate, effectiveEndDate);
        return ResponseEntity.ok(list);
    }

        /**
     * Retrieves daily conciliation case trend for Lupon reports within a date range.
     */
    @GetMapping("/reports/daily-trend")
    public ResponseEntity<List<DailyCaseTrendResponse>> getDailyTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDate effectiveEndDate = (endDate != null) ? endDate : LocalDate.now();
        LocalDate effectiveStartDate = (startDate != null) ? startDate : effectiveEndDate.minusDays(29);

        List<DailyCaseTrendResponse> list = luponReports.getDailyTrendReport(effectiveStartDate, effectiveEndDate);
        return ResponseEntity.ok(list);
    }

        /**
     * Retrieves case status breakdown distribution for Lupon reports within a date range.
     */
    @GetMapping("/reports/status-breakdown")
    public ResponseEntity<CaseStatusBreakdownResponse> getStatusBreakdown(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDate effectiveEndDate = (endDate != null) ? endDate : LocalDate.now();
        LocalDate effectiveStartDate = (startDate != null) ? startDate : effectiveEndDate.minusMonths(5).withDayOfMonth(1);

        CaseStatusBreakdownResponse response = luponReports.getStatusBreakdownReport(effectiveStartDate, effectiveEndDate);
        return ResponseEntity.ok(response);
    }

        /**
     * Retrieves paginated and filtered Lupong Tagapamayapa cases (referred/escalated only).
     */
    @GetMapping("/table")
    public ResponseEntity<PageResponse<LuponTableRowResponse>> getLuponCases(
            @ModelAttribute LuponFilterRequest filter) {
        PageResponse<LuponTableRowResponse> response = luponService.getLuponCases(filter);
        return ResponseEntity.ok(response);
    }

        /**
     * Retrieves summary KPI statistics for Lupong Tagapamayapa module.
     */
    @GetMapping("/stats")
    public ResponseEntity<LuponStatsResponse> getLuponStats() {
        LuponStatsResponse response = luponService.getLuponStats();
        return ResponseEntity.ok(response);
    }

        /**
     * Retrieves assigned Pangkat Tagapagkasundo panel members for a case.
     */
    @GetMapping("/{caseId}/pangkat")
    public ResponseEntity<List<LuponPanelMemberResponse>> getPangkatMembers(
            @PathVariable Long caseId) {
        List<LuponPanelMemberResponse> response = luponService.getPanelMembers(caseId);
        return ResponseEntity.ok(response);
    }

        /**
     * Lists all hearings (Mediation + Conciliation with Pangkat panel attendance) for a given Lupon case.
     */
    @GetMapping("/{caseId}/conciliation")
    public ResponseEntity<List<ConciliationSessionResponse>> getConciliationSessions(
            @PathVariable Long caseId) {
        List<ConciliationSessionResponse> response = conciliationService.getConciliationSessions(caseId);
        return ResponseEntity.ok(response);
    }

        /**
     * Schedules a new Conciliation Hearing for a Lupon case.
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
     */
    @GetMapping("/conciliation/{sessionId}/followup")
    public ResponseEntity<List<SessionFollowUpResponse>> getSessionFollowUps(
            @PathVariable Long sessionId) {
        List<SessionFollowUpResponse> response = conciliationService.getConciliationFollowUps(sessionId);
        return ResponseEntity.ok(response);
    }

        /**
     * Extends the Lupon conciliation period by 15 days (one-time allowable extension).
     */
    @PostMapping("/{caseId}/extend")
    @LogAudit(
            module = AuditModule.LUPON,
            action = "EXTEND_CONCILIATION_PERIOD",
            severity = Severity.INFO,
            captureNewValue = true
    )
    public ResponseEntity<BlotterCaseDetailResponse> extendLuponCase(
            @PathVariable Long caseId,
            @Valid @RequestBody ExtendCaseRequest request) {
        if (!SecurityUtils.hasPermission("Manage Conciliation")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to extend conciliation period.");
        }
        BlotterCaseDetailResponse response = luponService.extendLuponCase(caseId, request);
        return ResponseEntity.ok(response);
    }

        /**
     * Retrieves summary KPI metrics for Lupon Archive and Trash Vault.
     */
    @GetMapping("/archive-kpi")
    public ResponseEntity<BlotterLifecycleKpiResponse> getLuponLifecycleKpi() {
        BlotterLifecycleKpiResponse response = luponArchive.getLuponLifecycleKpi();
        return ResponseEntity.ok(response);
    }

        /**
     * Retrieves paginated archived Lupon cases for the Archive Table.
     */
    @GetMapping("/archive-table")
    public ResponseEntity<PageResponse<BlotterArchiveItemResponse>> getArchivedLuponCases(
            @ModelAttribute BlotterArchiveFilterRequest filter) {
        PageResponse<BlotterArchiveItemResponse> response = luponArchive.getArchivedLuponCases(filter);
        return ResponseEntity.ok(response);
    }

        /**
     * Retrieves paginated soft-deleted Lupon cases for the Recycle Bin / Trash Table.
     */
    @GetMapping("/recent-delete-table")
    public ResponseEntity<PageResponse<BlotterRecentDeleteItemResponse>> getRecentDeletedLuponCases(
            @ModelAttribute BlotterArchiveFilterRequest filter) {
        PageResponse<BlotterRecentDeleteItemResponse> response = luponArchive.getRecentDeletedLuponCases(filter);
        return ResponseEntity.ok(response);
    }

        /**
     * Archives a resolved or completed Lupon case.
     */
    @PatchMapping("/archive-case/{caseId}")
    @LogAudit(module = AuditModule.LUPON, action = "ARCHIVE_LUPON_CASE", severity = Severity.INFO,
            captureOldValue = true, oldValueArgIndex = 1, captureNewValue = false)
    public ResponseEntity<Void> archiveCase(
            @PathVariable Long caseId,
            @Valid @RequestBody ArchiveCaseRequest request) {
        if (!SecurityUtils.hasPermission("Archive Case")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to archive cases.");
        }
        luponArchive.archiveCase(caseId, request);
        return ResponseEntity.noContent().build();
    }

        /**
     * Restores an archived Lupon case back to active records.
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
