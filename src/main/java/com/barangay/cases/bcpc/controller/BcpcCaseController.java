package com.barangay.cases.bcpc.controller;

import com.barangay.cases.bcpc.dto.BcpcCarCaseCreateRequest;
import com.barangay.cases.bcpc.dto.BcpcCarCaseDetailResponse;
import com.barangay.cases.bcpc.dto.BcpcCarCaseUpdateRequest;
import com.barangay.cases.bcpc.dto.BcpcDashboardKpiResponse;
import com.barangay.cases.bcpc.dto.BcpcDashboardMonitoringItem;
import com.barangay.cases.bcpc.dto.BcpcCiclCaseCreateRequest;
import com.barangay.cases.bcpc.dto.BcpcCiclCaseDetailResponse;
import com.barangay.cases.bcpc.dto.BcpcCiclCaseUpdateRequest;
import com.barangay.cases.bcpc.dto.BcpcCustodyLogDto;
import com.barangay.cases.bcpc.dto.BcpcFilterRequest;
import com.barangay.cases.bcpc.dto.BcpcStatsResponse;
import com.barangay.cases.bcpc.dto.BcpcTableRowResponse;
import com.barangay.cases.bcpc.dto.BcpcCaseUniversalDetailResponse;
import com.barangay.cases.bcpc.dto.BcpcReferralCancelRequest;
import com.barangay.cases.bcpc.dto.BcpcCategoryComparisonResponse;
import com.barangay.cases.bcpc.dto.BcpcReferralCreateRequest;
import com.barangay.cases.bcpc.dto.BcpcReferralFeedbackRequest;
import com.barangay.cases.bcpc.dto.BcpcReferralResponse;
import com.barangay.cases.bcpc.dto.BcpcReportKpiResponse;
import com.barangay.cases.bcpc.dto.UpdateCiclCustodyLogDto;
import com.barangay.cases.bcpc.dto.BcpcInterventionDto;
import com.barangay.cases.bcpc.dto.BcpcInterventionCreateRequest;
import com.barangay.cases.bcpc.dto.BcpcInterventionUpdateRequest;
import com.barangay.cases.bcpc.dto.BcpcInterventionProgressRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.barangay.cases.bcpc.service.BcpcArchiveService;
import com.barangay.cases.bcpc.service.BcpcCaseService;
import com.barangay.cases.bcpc.service.BcpcCustodyService;
import com.barangay.cases.bcpc.service.BcpcInterventionService;
import com.barangay.cases.bcpc.service.BcpcReferralService;
import com.barangay.cases.blotter.dto.*;
import com.barangay.cases.blotter.service.CaseFileService;
import com.barangay.cases.shared.service.CaseTimelineService;
import com.barangay.zshared.audit.annotations.LogAudit;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;
import com.barangay.zshared.mapper.PageResponse;
import com.barangay.zshared.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Barangay Council for the Protection of Children (BCPC) operations.
 * Provides distinct and dedicated endpoints for:
 * 1. Child at Risk (CAR) cases
 * 2. Child in Conflict with the Law (CICL) cases
 * 3. File attachments (Narrative, Evidence, Witness)
 * 4. Archive & Trash Vault lifecycle operations
 */
@RestController
@RequestMapping("/api/v1/bcpc")
@RequiredArgsConstructor
public class BcpcCaseController {

    private final BcpcCaseService bcpcCaseService;
    private final BcpcArchiveService bcpcArchiveService;
    private final BcpcCustodyService bcpcCustodyService;
    private final BcpcReferralService bcpcReferralService;
    private final BcpcInterventionService bcpcInterventionService;
    private final CaseFileService caseFileService;
    private final CaseTimelineService caseTimelineService;

    /**
     * Retrieves BCPC KPI statistics (Total cases with month trend, active, referred, settled).
     * Excludes permanently deleted, deleted (trash), and archived cases.
     *
     * @return the BCPC KPI statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<BcpcStatsResponse> getStats() {
        BcpcStatsResponse response = bcpcCaseService.getBcpcStats();
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves BCPC Weekly Operational Dashboard KPIs.
     */
    @GetMapping("/dashboard-kpi")
    public ResponseEntity<BcpcDashboardKpiResponse> getDashboardKpi() {
        return ResponseEntity.ok(bcpcCaseService.getDashboardKpis());
    }

    /**
     * Retrieves top recent cases for the dashboard.
     */
    @GetMapping("/recent-cases")
    public ResponseEntity<List<BcpcTableRowResponse>> getRecentCases(
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ResponseEntity.ok(bcpcCaseService.getRecentCases(limit));
    }

    /**
     * Retrieves active monitoring items (pending referrals and active interventions).
     */
    @GetMapping("/monitoring-feed")
    public ResponseEntity<List<BcpcDashboardMonitoringItem>> getMonitoringFeed(
            @RequestParam(defaultValue = "6") int limit
    ) {
        return ResponseEntity.ok(bcpcCaseService.getMonitoringFeed(limit));
    }

    /**
     * Retrieves BCPC Report KPI metrics (Total cases, settled, active interventions, agency referrals)
     * within a specified date range.
     *
     * @param startDate start date filter
     * @param endDate   end date filter
     * @return {@link BcpcReportKpiResponse}
     */
    @GetMapping("/reports/kpi")
    public ResponseEntity<BcpcReportKpiResponse> getReportKpi(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDate effectiveStart = startDate != null ? startDate : LocalDate.now().minusMonths(6);
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        return ResponseEntity.ok(bcpcCaseService.getReportKpi(effectiveStart, effectiveEnd));
    }

    /**
     * Retrieves monthly case filing trend for BCPC reports within a date range.
     */
    @GetMapping("/reports/monthly-trend")
    public ResponseEntity<List<MonthlyCaseTrendResponse>> getMonthlyTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDate effectiveStart = startDate != null ? startDate : LocalDate.now().minusMonths(6);
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        return ResponseEntity.ok(bcpcCaseService.getMonthlyTrendReport(effectiveStart, effectiveEnd));
    }

    /**
     * Retrieves daily case filing trend for BCPC reports within a date range.
     */
    @GetMapping("/reports/daily-trend")
    public ResponseEntity<List<DailyCaseTrendResponse>> getDailyTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDate effectiveStart = startDate != null ? startDate : LocalDate.now().minusMonths(6);
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        return ResponseEntity.ok(bcpcCaseService.getDailyTrendReport(effectiveStart, effectiveEnd));
    }

    /**
     * Retrieves case status breakdown distribution for BCPC reports within a date range.
     */
    @GetMapping("/reports/status-breakdown")
    public ResponseEntity<CaseStatusBreakdownResponse> getStatusBreakdown(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDate effectiveStart = startDate != null ? startDate : LocalDate.now().minusMonths(6);
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        return ResponseEntity.ok(bcpcCaseService.getStatusBreakdownReport(effectiveStart, effectiveEnd));
    }

    /**
     * Retrieves child classification comparison (CAR vs CICL) for BCPC reports within a date range.
     */
    @GetMapping("/reports/category-comparison")
    public ResponseEntity<BcpcCategoryComparisonResponse> getCategoryComparison(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDate effectiveStart = startDate != null ? startDate : LocalDate.now().minusMonths(6);
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        return ResponseEntity.ok(bcpcCaseService.getCategoryComparisonReport(effectiveStart, effectiveEnd));
    }

    /**
     * Retrieves settlement efficiency performance metrics for BCPC cases within a date range.
     */
    @GetMapping("/reports/settlement-efficiency")
    public ResponseEntity<SettlementEfficiencyResponse> getSettlementEfficiency(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDate effectiveStart = startDate != null ? startDate : LocalDate.now().minusMonths(6);
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        return ResponseEntity.ok(bcpcCaseService.getSettlementEfficiencyReport(effectiveStart, effectiveEnd));
    }


    /**
     * Endpoint retrieving lifecycle and archive/trash statistics exclusively for the BCPC Department.
     *
     * @return {@link BlotterLifecycleKpiResponse}
     */
    @GetMapping("/archive-kpi")
    public ResponseEntity<BlotterLifecycleKpiResponse> getBcpcLifecycleKpi() {
        BlotterLifecycleKpiResponse response = bcpcArchiveService.getBcpcLifecycleKpi();
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint retrieving paginated archived BCPC cases for the Archive Table.
     *
     * @param filter {@link BlotterArchiveFilterRequest} search and filingType criteria
     * @return {@link PageResponse} of {@link BlotterArchiveItemResponse}
     */
    @GetMapping("/archive-table")
    public ResponseEntity<PageResponse<BlotterArchiveItemResponse>> getArchivedBcpcCases(
            @ModelAttribute BlotterArchiveFilterRequest filter) {
        PageResponse<BlotterArchiveItemResponse> response = bcpcArchiveService.getArchivedBcpcCases(filter);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint retrieving paginated soft-deleted BCPC cases for the Recent Delete / Trash Table.
     * Excludes permanently deleted records.
     *
     * @param filter {@link BlotterArchiveFilterRequest} search and filingType criteria
     * @return {@link PageResponse} of {@link BlotterRecentDeleteItemResponse}
     */
    @GetMapping("/recent-delete")
    public ResponseEntity<PageResponse<BlotterRecentDeleteItemResponse>> getRecentDeletedBcpcCases(
            @ModelAttribute BlotterArchiveFilterRequest filter) {
        PageResponse<BlotterRecentDeleteItemResponse> response = bcpcArchiveService.getRecentDeletedBcpcCases(filter);
        return ResponseEntity.ok(response);
    }

    /**
     * Archives an eligible BCPC case and its entire cycle chain.
     *
     * @param caseId  target case ID
     * @param request {@link ArchiveCaseRequest} containing archive remarks
     * @return 200 OK
     */
    @PatchMapping("/archive-case/{caseId}")
    @LogAudit(module = AuditModule.BCPC, action = "ARCHIVE_BCPC_CASE", severity = Severity.INFO)
    public ResponseEntity<Void> archiveCase(
            @PathVariable Long caseId,
            @RequestBody(required = false) ArchiveCaseRequest request) {
        if (!SecurityUtils.hasPermission("Archive Case")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to archive cases.");
        }
        bcpcArchiveService.archiveCase(caseId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Restores an archived BCPC case back to active management.
     *
     * @param caseId target case ID
     * @return 200 OK
     */
    @PatchMapping("/{caseId}/restore-archive")
    @LogAudit(module = AuditModule.BCPC, action = "RESTORE_BCPC_CASE", severity = Severity.INFO)
    public ResponseEntity<Void> restoreArchivedCase(@PathVariable Long caseId) {
        if (!SecurityUtils.hasPermission("Restore Case")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to restore cases.");
        }
        bcpcArchiveService.restoreArchivedCase(caseId);
        return ResponseEntity.ok().build();
    }

    /**
     * Restores a soft-deleted BCPC case from trash back to active records.
     *
     * @param caseId target case ID
     * @return 200 OK
     */
    @PatchMapping("/{caseId}/restore-trash")
    @LogAudit(module = AuditModule.BCPC, action = "RESTORE_BCPC_CASE_FROM_TRASH", severity = Severity.INFO)
    public ResponseEntity<Void> restoreTrashCase(@PathVariable Long caseId) {
        if (!SecurityUtils.hasPermission("Restore Case")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to restore cases.");
        }
        bcpcArchiveService.restoreArchivedCase(caseId);
        return ResponseEntity.ok().build();
    }

    /**
     * Moves a BCPC case to the recycle bin (soft-delete).
     *
     * @param caseId target case ID
     * @return success message
     */
    @PatchMapping("/{caseId}/soft-delete")
    @LogAudit(module = AuditModule.BCPC, action = "SOFT_DELETE_BCPC_CASE", severity = Severity.INFO)
    public ResponseEntity<Map<String, String>> softDelete(@PathVariable Long caseId) {
        if (!SecurityUtils.hasPermission("Delete Case")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to delete cases.");
        }
        bcpcArchiveService.softDelete(caseId);
        return ResponseEntity.ok(Map.of("message", "BCPC Case moved to trash successfully"));
    }

    /**
     * Permanently marks a soft-deleted BCPC case as purged (audit-safe permanent delete).
     *
     * @param caseId target case ID
     * @return success message
     */
    @DeleteMapping("/{caseId}/permanent-delete")
    @LogAudit(module = AuditModule.BCPC, action = "PERMANENT_DELETE_BCPC_CASE", severity = Severity.INFO)
    public ResponseEntity<Map<String, String>> permanentDelete(@PathVariable Long caseId) {
        if (!SecurityUtils.hasPermission("Permanent Delete Case")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to permanently delete cases.");
        }
        bcpcArchiveService.permanentDeleteCaseByFlag(caseId);
        return ResponseEntity.ok(Map.of("message", "BCPC Case permanently deleted"));
    }

    /**
     * Retrieves paginated and filtered BCPC case table rows.
     * Page size defaults to 5.
     *
     * @param filter the filtering criteria (search, status, category, page, size)
     * @return {@link PageResponse} of {@link BcpcTableRowResponse}
     */
    @GetMapping("/table")
    public ResponseEntity<PageResponse<BcpcTableRowResponse>> getBcpcTable(
            @ModelAttribute BcpcFilterRequest filter
    ) {
        PageResponse<BcpcTableRowResponse> response = bcpcCaseService.getBcpcCasesTable(filter);
        return ResponseEntity.ok(response);
    }


    /**
     * Retrieves complete universal case details by ID (automatically detects CAR or CICL).
     */
    @GetMapping("/{id}")
    public ResponseEntity<BcpcCaseUniversalDetailResponse> getCaseDetailsById(@PathVariable Long id) {
        if (!SecurityUtils.hasPermission("View Cases")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to view cases.");
        }
        BcpcCaseUniversalDetailResponse response = bcpcCaseService.getCaseDetailsById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates the status of a BCPC case (e.g. SETTLED, DISMISSED, WITHDRAWN) with optional remarks.
     *
     * @param id      the BCPC case ID
     * @param request payload containing target status and remarks/settlement terms
     * @return updated universal case details
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<BcpcCaseUniversalDetailResponse> updateCaseStatus(
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
            if ("CLOSED".equalsIgnoreCase(targetStatus) && !SecurityUtils.hasPermission("Close Case")) {
                throw new AccessDeniedException("Access Denied: You do not have permission to close cases.");
            }
        }
        BcpcCaseUniversalDetailResponse response = bcpcCaseService.updateCaseStatus(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the narrative statement attachment for a given BCPC case.
     */
    @GetMapping("/files/narrative/{caseId}")
    public ResponseEntity<byte[]> getNarrativeFile(@PathVariable Long caseId) {
        return caseFileService.getNarrativeFile(caseId);
    }

    /**
     * Retrieves the evidence file for a given BCPC evidence entry.
     */
    @GetMapping("/files/evidence/{evidenceId}")
    public ResponseEntity<byte[]> getEvidenceFile(@PathVariable Long evidenceId) {
        return caseFileService.getEvidenceFile(evidenceId);
    }

    /**
     * Retrieves the witness statement file for a given BCPC witness entry.
     */
    @GetMapping("/files/witness/{witnessId}")
    public ResponseEntity<byte[]> getWitnessFile(@PathVariable Long witnessId) {
        return caseFileService.getWitnessFile(witnessId);
    }

    /**
     * Retrieves all timeline events for a given BCPC case.
     */
    @GetMapping("/{caseId}/timeline")
    public ResponseEntity<java.util.List<TimelineEventResponse>> getCaseTimeline(@PathVariable Long caseId) {
        return ResponseEntity.ok(caseTimelineService.getTimelineForCase(caseId));
    }

    /**
     * Retrieves all confidential case notes for a given BCPC case.
     */
    @GetMapping("/{caseId}/notes")
    public ResponseEntity<java.util.List<CaseNoteResponse>> getCaseNotes(@PathVariable Long caseId) {
        return ResponseEntity.ok(bcpcCaseService.getCaseNotes(caseId));
    }

    /**
     * Adds a confidential case note to a given BCPC case.
     */
    @PostMapping("/{caseId}/notes")
    @LogAudit(module = AuditModule.BCPC, action = "ADD_CASE_NOTE", severity = Severity.INFO)
    public ResponseEntity<CaseNoteResponse> addCaseNote(
            @PathVariable Long caseId,
            @Valid @RequestBody CaseNoteRequest request
    ) {
        CaseNoteResponse response = bcpcCaseService.addCaseNote(caseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves the Custody & Turnover record for a BCPC case.
     */
    @GetMapping("/{caseId}/custody-log")
    public ResponseEntity<BcpcCustodyLogDto> getCustodyLog(@PathVariable Long caseId) {
        return ResponseEntity.ok(bcpcCustodyService.getCustodyLog(caseId));
    }

    /**
     * Records or updates a Custody & Turnover entry for a BCPC case (primarily CAR placement).
     */
    @PutMapping("/{caseId}/custody-log")
    @LogAudit(module = AuditModule.BCPC, action = "BCPC_CUSTODY_TURNOVER_RECORDED", severity = Severity.INFO)
    public ResponseEntity<BcpcCustodyLogDto> updateCustodyLog(
            @PathVariable Long caseId,
            @RequestBody BcpcCustodyLogDto request
    ) {
        BcpcCustodyLogDto response = bcpcCustodyService.updateCustodyLog(caseId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Records or updates a CICL Custody / Apprehension log.
     */
    @PutMapping("/{caseId}/cicl-custody-log")
    @LogAudit(module = AuditModule.BCPC, action = "CICL_CUSTODY_RECORDED", severity = Severity.INFO)
    public ResponseEntity<BcpcCustodyLogDto> updateCiclCustodyLog(
            @PathVariable Long caseId,
            @RequestBody UpdateCiclCustodyLogDto request
    ) {
        BcpcCustodyLogDto response = bcpcCustodyService.updateCiclCustodyLog(caseId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all inter-agency referrals for a BCPC case.
     */
    @GetMapping("/{caseId}/referrals")
    public ResponseEntity<List<BcpcReferralResponse>> getReferrals(@PathVariable Long caseId) {
        return ResponseEntity.ok(bcpcReferralService.getReferrals(caseId));
    }

    /**
     * Issues an official inter-agency referral.
     */
    @PostMapping("/{caseId}/referrals")
    @LogAudit(module = AuditModule.BCPC, action = "BCPC_REFERRAL_ISSUED", severity = Severity.INFO)
    public ResponseEntity<BcpcReferralResponse> issueReferral(
            @PathVariable Long caseId,
            @Valid @RequestBody BcpcReferralCreateRequest request
    ) {
        BcpcReferralResponse response = bcpcReferralService.issueReferral(caseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Records official feedback received from receiving agency.
     */
    @PostMapping("/{caseId}/referrals/{referralId}/feedback")
    @LogAudit(module = AuditModule.BCPC, action = "BCPC_REFERRAL_FEEDBACK_RECORDED", severity = Severity.INFO)
    public ResponseEntity<BcpcReferralResponse> recordReferralFeedback(
            @PathVariable Long caseId,
            @PathVariable Long referralId,
            @Valid @RequestBody BcpcReferralFeedbackRequest request
    ) {
        BcpcReferralResponse response = bcpcReferralService.recordReferralFeedback(caseId, referralId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancels an issued referral.
     */
    @PostMapping("/{caseId}/referrals/{referralId}/cancel")
    @LogAudit(module = AuditModule.BCPC, action = "BCPC_REFERRAL_CANCELLED", severity = Severity.INFO)
    public ResponseEntity<BcpcReferralResponse> cancelReferral(
            @PathVariable Long caseId,
            @PathVariable Long referralId,
            @Valid @RequestBody BcpcReferralCancelRequest request
    ) {
        BcpcReferralResponse response = bcpcReferralService.cancelReferral(caseId, referralId, request);
        return ResponseEntity.ok(response);
    }


    /**
     * Retrieves all intervention programs and monitoring records for a BCPC case.
     */
    @GetMapping("/{caseId}/interventions")
    public ResponseEntity<List<BcpcInterventionDto>> getInterventions(
            @PathVariable Long caseId
    ) {
        List<BcpcInterventionDto> list = bcpcInterventionService.getInterventionsByCaseId(caseId);
        return ResponseEntity.ok(list);
    }

    /**
     * Initiates a new intervention plan for a child under a BCPC case.
     */
    @PostMapping("/{caseId}/interventions")
    @LogAudit(module = AuditModule.BCPC, action = "BCPC_INTERVENTION_CREATED", severity = Severity.INFO)
    public ResponseEntity<BcpcInterventionDto> createIntervention(
            @PathVariable Long caseId,
            @Valid @RequestBody BcpcInterventionCreateRequest request
    ) {
        BcpcInterventionDto response = bcpcInterventionService.createIntervention(caseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an intervention plan details.
     */
    @PutMapping("/{caseId}/interventions/{interventionId}")
    @LogAudit(module = AuditModule.BCPC, action = "BCPC_INTERVENTION_UPDATED", severity = Severity.INFO)
    public ResponseEntity<BcpcInterventionDto> updateIntervention(
            @PathVariable Long caseId,
            @PathVariable Long interventionId,
            @Valid @RequestBody BcpcInterventionUpdateRequest request
    ) {
        BcpcInterventionDto response = bcpcInterventionService.updateIntervention(caseId, interventionId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Records a new monitoring session or progress log entry for an active intervention.
     */
    @PostMapping("/{caseId}/interventions/{interventionId}/progress")
    @LogAudit(module = AuditModule.BCPC, action = "BCPC_INTERVENTION_PROGRESS_RECORDED", severity = Severity.INFO)
    public ResponseEntity<BcpcInterventionDto> recordInterventionProgress(
            @PathVariable Long caseId,
            @PathVariable Long interventionId,
            @Valid @RequestBody BcpcInterventionProgressRequest request
    ) {
        BcpcInterventionDto response = bcpcInterventionService.recordInterventionProgress(caseId, interventionId, request);
        return ResponseEntity.ok(response);
    }
}
