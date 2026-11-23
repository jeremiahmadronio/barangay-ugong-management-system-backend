package com.barangay.cases.vawc.controller;

import com.barangay.cases.shared.service.CaseNoteService;
import com.barangay.cases.blotter.dto.CaseNoteRequest;
import com.barangay.cases.blotter.dto.CaseNoteResponse;
import com.barangay.cases.blotter.service.CaseFileService;
import com.barangay.cases.blotter.dto.CaseStatusBreakdownResponse;
import com.barangay.cases.blotter.dto.DailyCaseTrendResponse;
import com.barangay.cases.blotter.dto.MonthlyCaseTrendResponse;
import com.barangay.cases.blotter.dto.*;
import com.barangay.cases.vawc.dto.*;
import com.barangay.cases.vawc.service.VawcArchiveService;
import com.barangay.cases.vawc.service.VawcCaseService;
import com.barangay.cases.vawc.service.VawcReportService;
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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/vawc")
@RequiredArgsConstructor
public class VawcCaseController {

    private final VawcCaseService vawcCaseService;
    private final VawcReportService vawcReportService;
    private final VawcArchiveService vawcArchiveService;
    private final CaseFileService caseFileService;
    private final CaseNoteService caseNoteService;

    /**
     * Retrieves all VAWC case records.
     *
     * @return list of {@link VawcCaseDetailResponse}
     */
    @GetMapping
    public ResponseEntity<List<VawcCaseDetailResponse>> getAllVawcCases() {
        List<VawcCaseDetailResponse> cases = vawcCaseService.getAllVawcCases();
        return ResponseEntity.ok(cases);
    }

    /**
     * Retrieves paginated and filtered VAWC case table rows.
     *
     * @param filter the filtering criteria (search, status, riskLevel, page, size)
     * @return {@link PageResponse} of {@link VawcTableRowResponse}
     */
    @GetMapping("/table")
    public ResponseEntity<PageResponse<VawcTableRowResponse>> getVawcTable(
            @ModelAttribute VawcFilterRequest filter
    ) {
        PageResponse<VawcTableRowResponse> response = vawcCaseService.getVawcCases(filter);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves VAWC KPI Statistics with trends.
     *
     * @return {@link VawcStatsResponse}
     */
    @GetMapping("/stats")
    public ResponseEntity<VawcStatsResponse> getVawcStats() {
        VawcStatsResponse stats = vawcCaseService.getVawcStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Retrieves the 4 core Operational Dashboard KPI metrics for VAWC Desk:
     * (Total Cases This Week, Total Active BPOs This Week, Total Referred Cases This Week, Total Withdrawn Cases This Week).
     *
     * @return {@link ResponseEntity} of {@link VawcDashboardKpiResponse}
     */
    @GetMapping("/dashboard-kpi")
    public ResponseEntity<VawcDashboardKpiResponse> getDashboardKpis() {
        VawcDashboardKpiResponse response = vawcReportService.getDashboardKpis();
        return ResponseEntity.ok(response);
    }


    /**
     * Retrieves date-filtered KPI Statistics for VAWC Reports.
     */
    @GetMapping("/reports/kpi")
    public ResponseEntity<VawcReportKpiResponse> getReportKpi(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        VawcReportKpiResponse response = vawcReportService.getReportKpi(startDate, endDate);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves Inter-Agency Referral distribution breakdown.
     */
    @GetMapping("/reports/agency-referrals")
    public ResponseEntity<List<VawcAgencyReferralBreakdownResponse>> getAgencyReferralBreakdown(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<VawcAgencyReferralBreakdownResponse> response = vawcReportService.getAgencyReferralBreakdown(startDate, endDate);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves monthly case trend report for VAWC cases.
     */
    @GetMapping("/reports/monthly-trend")
    public ResponseEntity<List<MonthlyCaseTrendResponse>> getMonthlyTrendReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<MonthlyCaseTrendResponse> response = vawcReportService.getMonthlyTrendReport(startDate, endDate);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves daily case trend report for VAWC cases.
     */
    @GetMapping("/reports/daily-trend")
    public ResponseEntity<List<DailyCaseTrendResponse>> getDailyTrendReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<DailyCaseTrendResponse> response = vawcReportService.getDailyTrendReport(startDate, endDate);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves case status breakdown report for VAWC cases.
     */
    @GetMapping("/reports/status-breakdown")
    public ResponseEntity<CaseStatusBreakdownResponse> getStatusBreakdownReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        CaseStatusBreakdownResponse response = vawcReportService.getStatusBreakdownReport(startDate, endDate);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves table records within date range for VAWC reports.
     */
    @GetMapping("/reports/records")
    public ResponseEntity<List<VawcTableRowResponse>> getVawcReportRecords(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<VawcTableRowResponse> response = vawcReportService.getVawcReportRecords(startDate, endDate);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new VAWC Case entry (Intake / Case Filing).
     *
     * @param request the case creation payload containing all complaint details
     * @return {@link ResponseEntity} containing the created case details with HTTP status 201 (Created)
     */
    @PostMapping
    @LogAudit(module = AuditModule.VAWC, action = "CREATE_VAWC_CASE", severity = Severity.INFO)
    public ResponseEntity<VawcCaseDetailResponse> createVawcCase(@Valid @RequestBody VawcCaseCreateRequest request) {
        if (!SecurityUtils.hasPermission("File Complaint")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to file complaints.");
        }
        VawcCaseDetailResponse response = vawcCaseService.createVawcCase(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing VAWC Case entry.
     */
    @PutMapping("/{caseId}")
    @LogAudit(module = AuditModule.VAWC, action = "UPDATE_VAWC_CASE", severity = Severity.INFO)
    public ResponseEntity<VawcCaseDetailResponse> updateVawcCase(
            @PathVariable Long caseId,
            @Valid @RequestBody VawcCaseUpdateRequest request
    ) {
        if (!SecurityUtils.hasPermission("Edit Case")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to edit cases.");
        }
        VawcCaseDetailResponse response = vawcCaseService.updateVawcCase(caseId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates the status of a VAWC case (e.g. SETTLED, DISMISSED) with optional status remarks.
     *
     * @param caseId the VAWC case ID
     * @param request the status update request payload containing status and remarks
     * @return updated {@link VawcCaseDetailResponse}
     */
    @PatchMapping("/{caseId}/status")
    @LogAudit(module = AuditModule.VAWC, action = "UPDATE_VAWC_STATUS", severity = Severity.INFO)
    public ResponseEntity<VawcCaseDetailResponse> updateCaseStatus(
            @PathVariable Long caseId,
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
        }
        return ResponseEntity.ok(vawcCaseService.updateCaseStatus(caseId, request));
    }

    /**
     * Retrieves a single VAWC case detail by ID.
     *
     * @param id the case ID
     * @return {@link VawcCaseDetailResponse}
     */
    @GetMapping({ "/{id}", "/details/{id}" })
    public ResponseEntity<VawcCaseDetailResponse> getVawcCaseById(@PathVariable Long id) {
        if (!SecurityUtils.hasPermission("View Cases")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to view cases.");
        }
        VawcCaseDetailResponse response = vawcCaseService.getVawcCaseById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the narrative statement attachment for a given VAWC case.
     */
    @GetMapping("/files/narrative/{caseId}")
    public ResponseEntity<byte[]> getNarrativeFile(@PathVariable Long caseId) {
        return caseFileService.getNarrativeFile(caseId);
    }

    /**
     * Retrieves the evidence file for a given VAWC evidence entry.
     */
    @GetMapping("/files/evidence/{evidenceId}")
    public ResponseEntity<byte[]> getEvidenceFile(@PathVariable Long evidenceId) {
        return caseFileService.getEvidenceFile(evidenceId);
    }

    /**
     * Retrieves the witness statement file for a given VAWC witness entry.
     */
    @GetMapping("/files/witness/{witnessId}")
    public ResponseEntity<byte[]> getWitnessFile(@PathVariable Long witnessId) {
        return caseFileService.getWitnessFile(witnessId);
    }

    /**
     * Adds a note to a VAWC case.
     */
    @PostMapping("/{caseId}/notes")
    @LogAudit(module = AuditModule.VAWC, action = "ADD_VAWC_CASE_NOTE", severity = Severity.INFO)
    public ResponseEntity<CaseNoteResponse> addCaseNote(
            @PathVariable Long caseId,
            @Valid @RequestBody CaseNoteRequest request) {
        if (!SecurityUtils.hasPermission("Manage Case Notes")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to manage case notes.");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(caseNoteService.addCaseNote(caseId, request));
    }

    /**
     * Lists all notes for a VAWC case.
     */
    @GetMapping("/{caseId}/notes")
    public ResponseEntity<List<CaseNoteResponse>> getCaseNotes(
            @PathVariable Long caseId) {
        if (!SecurityUtils.hasPermission("Manage Case Notes")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to view case notes.");
        }
        return ResponseEntity.ok(caseNoteService.getCaseNotes(caseId));
    }

    /**
     * Retrieves timeline events for a VAWC case.
     */
    @GetMapping("/{caseId}/timeline")
    public ResponseEntity<List<TimelineEventResponse>> getCaseTimeline(
            @PathVariable Long caseId) {
        return ResponseEntity.ok(vawcCaseService.getCaseTimeline(caseId));
    }

    /**
     * Activates a Barangay Protection Order (BPO) for the given VAWC case.
     */
    @PostMapping("/{caseId}/bpo/activate")
    @LogAudit(module = AuditModule.VAWC, action = "ACTIVATE_BPO", severity = Severity.INFO)
    public ResponseEntity<VawcCaseDetailResponse> activateBpo(@PathVariable Long caseId) {
        if (!SecurityUtils.hasPermission("Issue Barangay Protection Order (BPO)")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to manage protection orders.");
        }
        return ResponseEntity.ok(vawcCaseService.activateBpo(caseId));
    }

    /**
     * Records service of a Barangay Protection Order (BPO) to the respondent.
     */
    @PostMapping("/{caseId}/bpo/serve")
    @LogAudit(module = AuditModule.VAWC, action = "SERVE_BPO", severity = Severity.INFO)
    public ResponseEntity<VawcCaseDetailResponse> serveBpo(
            @PathVariable Long caseId,
            @Valid @RequestBody BpoServeRequest request) {
        if (!SecurityUtils.hasPermission("Issue Barangay Protection Order (BPO)")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to manage protection orders.");
        }
        return ResponseEntity.ok(vawcCaseService.serveBpo(caseId, request));
    }

    /**
     * Issues a formal referral for a VAWC case to a partner agency (DSWD, PNP, PAO, Medical, etc.).
     */
    @PostMapping("/{caseId}/referrals")
    @LogAudit(module = AuditModule.VAWC, action = "ISSUE_VAWC_REFERRAL", severity = Severity.INFO)
    public ResponseEntity<VawcReferralResponse> createReferral(
            @PathVariable Long caseId,
            @Valid @RequestBody VawcReferralCreateRequest request) {
        if (!SecurityUtils.hasPermission("Manage Referral")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to manage referrals.");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vawcCaseService.createReferral(caseId, request));
    }

    /**
     * Lists all referrals and agency feedback for a VAWC case.
     */
    @GetMapping("/{caseId}/referrals")
    public ResponseEntity<List<VawcReferralResponse>> getReferrals(
            @PathVariable Long caseId) {
        return ResponseEntity.ok(vawcCaseService.getReferralsByCaseId(caseId));
    }

    /**
     * Records partner agency feedback and outcome for a VAWC referral.
     */
    @PostMapping("/{caseId}/referrals/{referralId}/feedback")
    @LogAudit(module = AuditModule.VAWC, action = "RECORD_REFERRAL_FEEDBACK", severity = Severity.INFO)
    public ResponseEntity<VawcReferralResponse> addReferralFeedback(
            @PathVariable Long caseId,
            @PathVariable Long referralId,
            @Valid @RequestBody ReferralFeedbackRequest request) {
        if (!SecurityUtils.hasPermission("Manage Referral")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to manage referrals.");
        }
        return ResponseEntity.ok(vawcCaseService.addReferralFeedback(caseId, referralId, request));
    }


    /**
     * Endpoint retrieving lifecycle and trash statistics exclusively for the VAWC Department.
     *
     * @return {@link BlotterLifecycleKpiResponse}
     */
    @GetMapping("/archive-kpi")
    public ResponseEntity<BlotterLifecycleKpiResponse> getVawcLifecycleKpi() {
        BlotterLifecycleKpiResponse response = vawcArchiveService.getVawcLifecycleKpi();
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint retrieving paginated archived VAWC cases for the Archive Table.
     *
     * @param filter {@link BlotterArchiveFilterRequest} search and filingType criteria
     * @return {@link PageResponse} of {@link BlotterArchiveItemResponse}
     */
    @GetMapping("/archive-table")
    public ResponseEntity<PageResponse<BlotterArchiveItemResponse>> getArchivedVawcCases(
            @ModelAttribute BlotterArchiveFilterRequest filter) {
        PageResponse<BlotterArchiveItemResponse> response = vawcArchiveService.getArchivedVawcCases(filter);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint retrieving paginated soft-deleted VAWC cases for the Recent Delete / Trash Table.
     * Excludes permanently deleted records.
     *
     * @param filter {@link BlotterArchiveFilterRequest} search and filingType criteria
     * @return {@link PageResponse} of {@link BlotterRecentDeleteItemResponse}
     */
    @GetMapping("/recent-delete")
    public ResponseEntity<PageResponse<BlotterRecentDeleteItemResponse>> getRecentDeletedVawcCases(
            @ModelAttribute BlotterArchiveFilterRequest filter) {
        PageResponse<BlotterRecentDeleteItemResponse> response = vawcArchiveService.getRecentDeletedVawcCases(filter);
        return ResponseEntity.ok(response);
    }

    /**
     * Archives a resolved, settled, dismissed, or withdrawn VAWC case.
     *
     * @param caseId  target case ID
     * @param request archive remarks payload
     * @return 200 OK
     */
    @PatchMapping("/archive-case/{caseId}")
    @LogAudit(module = AuditModule.VAWC, action = "ARCHIVE_VAWC_CASE", severity = Severity.INFO,
            captureOldValue = true, oldValueArgIndex = 1, captureNewValue = false)
    public ResponseEntity<Void> archiveCase(
            @PathVariable Long caseId,
            @Valid @RequestBody ArchiveCaseRequest request
    ) {
        if (!SecurityUtils.hasPermission("Archive Case")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to archive cases.");
        }
        vawcArchiveService.archiveCase(caseId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Restores an archived VAWC case back to active records.
     *
     * @param caseId target case ID
     * @return 200 OK
     */
    @PatchMapping({"/{caseId}/restore-archive", "/{caseId}/restore-trash"})
    @LogAudit(module = AuditModule.VAWC, action = "RESTORE_VAWC_CASE", severity = Severity.INFO,
            captureOldValue = true, oldValueArgIndex = 0, captureNewValue = false)
    public ResponseEntity<Void> restoreArchivedCase(@PathVariable Long caseId) {
        if (!SecurityUtils.hasPermission("Restore Case")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to restore cases.");
        }
        vawcArchiveService.restoreArchivedCase(caseId);
        return ResponseEntity.ok().build();
    }

    /**
     * Moves a VAWC case to the recycle bin (soft delete).
     *
     * @param caseId target case ID
     * @return success message
     */
    @PatchMapping("/{caseId}/soft-delete")
    @LogAudit(module = AuditModule.VAWC, action = "SOFT_DELETE_VAWC_CASE", severity = Severity.INFO,
            captureOldValue = true, oldValueArgIndex = 0)
    public ResponseEntity<Map<String, String>> softDelete(@PathVariable Long caseId) {
        if (!SecurityUtils.hasPermission("Delete Case")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to delete cases.");
        }
        vawcArchiveService.softDelete(caseId);
        return ResponseEntity.ok(Map.of("message", "VAWC Case moved to trash successfully"));
    }

    /**
     * Permanently purges a soft-deleted VAWC case from the recycle bin.
     *
     * @param caseId target case ID
     * @return success message
     */
    @DeleteMapping("/{caseId}/permanent-delete")
    @LogAudit(module = AuditModule.VAWC, action = "PERMANENT_DELETE_VAWC_CASE", severity = Severity.INFO,
            captureOldValue = true, oldValueArgIndex = 0)
    public ResponseEntity<Map<String, String>> permanentDelete(@PathVariable Long caseId) {
        if (!SecurityUtils.hasPermission("Permanent Delete Case")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to permanently delete cases.");
        }
        vawcArchiveService.permanentDeleteCaseByFlag(caseId);
        return ResponseEntity.ok(Map.of("message", "VAWC Case permanently deleted"));
    }
}
