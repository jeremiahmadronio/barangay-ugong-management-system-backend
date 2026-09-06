package com.barangay.cases.vawc.controller;

import com.barangay.cases.shared.service.CaseNoteService;
import com.barangay.cases.blotter.dto.CaseNoteRequest;
import com.barangay.cases.blotter.dto.CaseNoteResponse;
import com.barangay.cases.blotter.service.CaseFileService;
import com.barangay.cases.blotter.dto.CaseStatusBreakdownResponse;
import com.barangay.cases.blotter.dto.DailyCaseTrendResponse;
import com.barangay.cases.blotter.dto.MonthlyCaseTrendResponse;
import com.barangay.cases.vawc.dto.*;
import com.barangay.cases.vawc.service.VawcCaseService;
import com.barangay.cases.vawc.service.VawcReportService;
import com.barangay.zshared.audit.annotations.LogAudit;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vawc")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class VawcCaseController {

    private final VawcCaseService vawcCaseService;
    private final VawcReportService vawcReportService;
    private final CaseFileService caseFileService;
    private final CaseNoteService caseNoteService;

    /**
     * Retrieves all VAWC case records.
     *
     * @return list of {@link VawcCaseDetailResponse}
     */
    @GetMapping
    @LogAudit(module = AuditModule.VAWC, action = "LIST_VAWC_CASES", severity = Severity.INFO)
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
    @LogAudit(module = AuditModule.VAWC, action = "LIST_VAWC_CASES_TABLE", severity = Severity.INFO)
    public ResponseEntity<com.barangay.zshared.mapper.PageResponse<com.barangay.cases.vawc.dto.VawcTableRowResponse>> getVawcTable(
            @ModelAttribute com.barangay.cases.vawc.dto.VawcFilterRequest filter
    ) {
        com.barangay.zshared.mapper.PageResponse<com.barangay.cases.vawc.dto.VawcTableRowResponse> response = vawcCaseService.getVawcCases(filter);
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

    // ==========================================
    // VAWC REPORTS & ANALYTICS ENDPOINTS
    // ==========================================

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
        VawcCaseDetailResponse response = vawcCaseService.createVawcCase(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves a single VAWC case detail by ID.
     *
     * @param id the case ID
     * @return {@link VawcCaseDetailResponse}
     */
    @GetMapping({ "/{id}", "/details/{id}" })
    @LogAudit(module = AuditModule.VAWC, action = "VIEW_VAWC_CASE", severity = Severity.INFO)
    public ResponseEntity<VawcCaseDetailResponse> getVawcCaseById(@PathVariable Long id) {
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
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(caseNoteService.addCaseNote(caseId, request));
    }

    /**
     * Lists all notes for a VAWC case.
     */
    @GetMapping("/{caseId}/notes")
    public ResponseEntity<List<CaseNoteResponse>> getCaseNotes(
            @PathVariable Long caseId) {
        return ResponseEntity.ok(caseNoteService.getCaseNotes(caseId));
    }

    /**
     * Retrieves timeline events for a VAWC case.
     */
    @GetMapping("/{caseId}/timeline")
    public ResponseEntity<List<com.barangay.cases.blotter.dto.TimelineEventResponse>> getCaseTimeline(
            @PathVariable Long caseId) {
        return ResponseEntity.ok(vawcCaseService.getCaseTimeline(caseId));
    }

    /**
     * Activates a Barangay Protection Order (BPO) for the given VAWC case.
     */
    @PostMapping("/{caseId}/bpo/activate")
    @LogAudit(module = AuditModule.VAWC, action = "ACTIVATE_BPO", severity = Severity.INFO)
    public ResponseEntity<VawcCaseDetailResponse> activateBpo(@PathVariable Long caseId) {
        return ResponseEntity.ok(vawcCaseService.activateBpo(caseId));
    }

    /**
     * Records service of a Barangay Protection Order (BPO) to the respondent.
     */
    @PostMapping("/{caseId}/bpo/serve")
    @LogAudit(module = AuditModule.VAWC, action = "SERVE_BPO", severity = Severity.INFO)
    public ResponseEntity<VawcCaseDetailResponse> serveBpo(
            @PathVariable Long caseId,
            @Valid @RequestBody com.barangay.cases.vawc.dto.BpoServeRequest request) {
        return ResponseEntity.ok(vawcCaseService.serveBpo(caseId, request));
    }

    /**
     * Issues a formal referral for a VAWC case to a partner agency (DSWD, PNP, PAO, Medical, etc.).
     */
    @PostMapping("/{caseId}/referrals")
    @LogAudit(module = AuditModule.VAWC, action = "ISSUE_VAWC_REFERRAL", severity = Severity.INFO)
    public ResponseEntity<com.barangay.cases.vawc.dto.VawcReferralResponse> createReferral(
            @PathVariable Long caseId,
            @Valid @RequestBody com.barangay.cases.vawc.dto.VawcReferralCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vawcCaseService.createReferral(caseId, request));
    }

    /**
     * Lists all referrals and agency feedback for a VAWC case.
     */
    @GetMapping("/{caseId}/referrals")
    public ResponseEntity<List<com.barangay.cases.vawc.dto.VawcReferralResponse>> getReferrals(
            @PathVariable Long caseId) {
        return ResponseEntity.ok(vawcCaseService.getReferralsByCaseId(caseId));
    }

    /**
     * Records partner agency feedback and outcome for a VAWC referral.
     */
    @PostMapping("/{caseId}/referrals/{referralId}/feedback")
    @LogAudit(module = AuditModule.VAWC, action = "RECORD_REFERRAL_FEEDBACK", severity = Severity.INFO)
    public ResponseEntity<com.barangay.cases.vawc.dto.VawcReferralResponse> addReferralFeedback(
            @PathVariable Long caseId,
            @PathVariable Long referralId,
            @Valid @RequestBody com.barangay.cases.vawc.dto.ReferralFeedbackRequest request) {
        return ResponseEntity.ok(vawcCaseService.addReferralFeedback(caseId, referralId, request));
    }
}
