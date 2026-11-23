package com.barangay.ftjs.controller;

import java.time.LocalDate;
import java.util.List;

import com.barangay.ftjs.dto.*;
import com.barangay.ftjs.service.*;
import com.barangay.zshared.mapper.PageResponse;
import jakarta.validation.Valid;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Param;
import org.springframework.data.web.PageableDefault;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import com.barangay.zshared.enumerated.FtjsStatus;
import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.audit.annotations.LogAudit;
import com.barangay.zshared.audit.service.AuditLogService;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;
import com.barangay.zshared.security.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for First Time Job Seeker endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ftjs")
@RequiredArgsConstructor
public class FtjsController {

    private final FtjsService ftjsService;
    private final FtjsReIssuanceService ftjsReIssuanceService;
    private final FtjsNoteService ftjsNoteService;
    private final FtjsArchiveAndDeleteService archiveAndDeleteService;
    private final FtjsRetentionService retentionService;
    private final FtjsReportsService ftjsReportsService;

    /**
     * Search residents by query string.
     */
    @GetMapping("/search-resident")
    public ResponseEntity<List<FtjsResidentSearchResponse>> searchResidents(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String search) {
        String searchTerm = (query != null && !query.isBlank()) ? query : search;
        return ResponseEntity.ok(ftjsService.searchResidents(searchTerm));
    }

    /**
     * Get FTJS dashboard statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<FtjsStatsResponse> getStats() {
        return ResponseEntity.ok(ftjsService.getStats());
    }

    /**
     * Get paginated FTJS management table.
     */
    @GetMapping("/summary")
    public ResponseEntity<Page<FtjsSummaryResponse>> getSummaryTable(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) FtjsStatus status,
            @RequestParam(required = false) String purpose,
            @RequestParam(required = false) Integer issuanceCount,
            @PageableDefault(size = 5, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ftjsService.getSummaryTable(search, status, purpose, issuanceCount, pageable));
    }

    /**
     * Get FTJS profile of a resident.
     */
    @GetMapping("/resident/{residentId}")
    public ResponseEntity<FtjsResidentSearchResponse> getResidentFtjsProfile(@PathVariable Long residentId) {
        return ResponseEntity.ok(ftjsService.getResidentFtjsProfile(residentId));
    }

    /**
     * Create a new FTJS certificate entry.
     */
    @PostMapping("/entry")
    @LogAudit(module = AuditModule.FTJS, action = "CREATE_FTJS_ENTRY", severity = Severity.INFO)
    public ResponseEntity<FtjsResidentSearchResponse> createFtjsEntry(@RequestBody FtjsRequestEntry request) {
        if (!SecurityUtils.hasPermission("Issue FTJS")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to issue FTJS certificates.");
        }
        log.info("Received FTJS application entry creation request for Resident ID: {}", request.residentId());
        FtjsResidentSearchResponse response = ftjsService.createFtjsEntry(request);

        return ResponseEntity.ok(response);
    }

    /**
     * Get FTJS applicant detail by ID.
     */
    @GetMapping("/detail/{id}")
    public ResponseEntity<FtjsDetailResponse> getFtjsDetail(@PathVariable Long id) {
        if (!SecurityUtils.hasPermission("View FTJS")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to view FTJS details.");
        }
        FtjsDetailResponse ftjs = ftjsService.getFtjsApplicantDetail(id);
        return ResponseEntity.ok(ftjs);
    }

    /**
     * Get linked blotter details for re-issuance.
     */
    @GetMapping("/linked-blotter/{ftjsId}")
    public ResponseEntity<LinkedBlotterResponseDTO> getLinkedBlotter(@PathVariable Long ftjsId) {
        return ResponseEntity.ok(ftjsReIssuanceService.getLinkedBlotter(ftjsId));
    }

    /**
     * Request FTJS certificate re-issuance.
     */
    @PostMapping("/re-issuance-request/{ftjsId}")
    @LogAudit(module = AuditModule.FTJS, action = "CREATE_RE-ISSUANCE_REQUEST", severity = Severity.INFO)
    public ResponseEntity<ReIssuanceRequestDto> createReIssuanceRequest(@PathVariable Long ftjsId, @RequestBody ReIssuanceRequestDto request) throws BadRequestException {
        if (!SecurityUtils.hasPermission("Re-issue FTJS")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to re-issue FTJS certificates.");
        }
        ReIssuanceRequestDto reIssuanceReq = ftjsReIssuanceService.createReIssuanceRequest(ftjsId, request);
        return ResponseEntity.ok(reIssuanceReq);
    }

    /**
     * Get re-issuance history by FTJS ID.
     */
    @GetMapping("/{ftjsId}/re-issuances/history")
    public ResponseEntity<List<ReIssuanceHistoryDto>> getReIssuanceHistory(@PathVariable Long ftjsId) {
        return ResponseEntity.ok(ftjsReIssuanceService.getReIssuanceHistory(ftjsId));
    }

    /**
     * Get FTJS timeline events by FTJS ID.
     */
    @GetMapping("/{ftjsId}/timeline/")
    public ResponseEntity<List<FtjsTimelineDto>> getTimeline(@PathVariable Long ftjsId) {
        return ResponseEntity.ok(ftjsReIssuanceService.getFtjsTimeline(ftjsId));
    }

    /**
     * Add a note to an FTJS record.
     */
    @PostMapping("/notes-entry/{ftjsId}")
    @LogAudit(module = AuditModule.FTJS, action = "ADD_FTJS_NOTE", severity = Severity.INFO,
            captureOldValue = true, oldValueArgIndex = 1, captureNewValue = true)
    public ResponseEntity<FtjsNoteResponse> addNote(@PathVariable Long ftjsId, @Valid @RequestBody FtjsNoteRequest request) {
        if (!SecurityUtils.hasPermission("Manage FTJS Notes")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to manage FTJS notes.");
        }
        FtjsNoteResponse response = ftjsNoteService.addNote(ftjsId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all notes for an FTJS record.
     */
    @GetMapping("/notes-history/{ftjsId}")
    public ResponseEntity<List<FtjsNoteResponse>> getNotes(@PathVariable Long ftjsId) {
        if (!SecurityUtils.hasPermission("Manage FTJS Notes")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to view or manage FTJS notes.");
        }
        List<FtjsNoteResponse> notes = ftjsNoteService.getNotesByFtjsId(ftjsId);
        return ResponseEntity.ok(notes);
    }

    /**
     * Archive an FTJS record.
     */
    @PostMapping("/archive/{id}")
    @LogAudit(module = AuditModule.FTJS, action = "ARCHIVE_FTJS_RECORD", severity = Severity.INFO,
            captureOldValue = true, oldValueArgIndex = 0, captureNewValue = false)
    public ResponseEntity<Void> archiveFtjs(@PathVariable Long id){
        if (!SecurityUtils.hasPermission("Archive FTJS")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to archive FTJS records.");
        }
        archiveAndDeleteService.archiveFtjs(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Restore an archived FTJS record.
     */
    @PostMapping("/restore/{id}")
    @LogAudit(module = AuditModule.FTJS, action = "RESTORE_FTJS_RECORD", severity = Severity.INFO,
            captureOldValue = true, oldValueArgIndex = 0, captureNewValue = false)
    public ResponseEntity<Void> restoreFtjs(@PathVariable Long id){
        if (!SecurityUtils.hasPermission("Restore FTJS")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to restore FTJS records.");
        }
        archiveAndDeleteService.restoreFtjs(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Get archive and trash statistics.
     */
    @GetMapping("/archive/stats")
    public ResponseEntity<FtjsArchiveStatsResponse> getArchiveAndDeleteStats(){
        FtjsArchiveStatsResponse stats = archiveAndDeleteService.getArchiveStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get table of archived FTJS records.
     */
    @GetMapping("/archives/summary")
    public ResponseEntity<Page<FtjsSummaryResponse>> getArchivedSummaryTable(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) FtjsStatus status,
            @RequestParam(required = false) String purpose,
            @RequestParam(required = false) Integer issuanceCount,
            @PageableDefault(size = 10, sort = "archivedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<FtjsSummaryResponse> response = archiveAndDeleteService.getArchivedSummaryTable(
                search, status, purpose, issuanceCount, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get table of deleted FTJS records.
     */
    @GetMapping("/trash/summary")
    public ResponseEntity<PageResponse<FtjsSummaryResponse>> getDeletedSummaryTable(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) FtjsStatus status,
            @RequestParam(required = false) String purpose,
            @RequestParam(required = false) Integer issuanceCount,
            @PageableDefault(size = 10, sort = "deleted_at", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<FtjsSummaryResponse> response = archiveAndDeleteService.getDeletedSummaryTable(
                search, status, purpose, issuanceCount, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get data retention setting.
     */
    @GetMapping("/display-retention")
    public ResponseEntity<RetentionSettingDto.Response> getRetentionSetting() {
        return ResponseEntity.ok(retentionService.getCurrentRetentionSetting());
    }

    /**
     * Update data retention setting.
     */
    @PutMapping("/update-retention")
    @LogAudit(module = AuditModule.FTJS, action = "UPDATE_FTJS_RETENTION_SETTING", severity = Severity.WARNING,
            captureOldValue = true, oldValueArgIndex = 0)
    public ResponseEntity<Void> updateRetentionSetting(@Valid @RequestBody RetentionSettingDto.Request request) {
        retentionService.updateRetentionSetting(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Soft delete an FTJS record.
     */
    @DeleteMapping("/{id}")
    @LogAudit(module = AuditModule.FTJS, action = "SOFT_DELETE_FTJS_RECORD", severity = Severity.LOW)
    public ResponseEntity<Void> softDelete(@PathVariable Long id) {
        if (!SecurityUtils.hasPermission("Delete FTJS")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to delete FTJS records.");
        }
        archiveAndDeleteService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Permanently delete an FTJS record.
     */
    @DeleteMapping("/trash/{id}/permanent")
    @LogAudit(module = AuditModule.FTJS, action = "PERMANENT_DELETE_FTJS_RECORD", severity = Severity.WARNING)
    public ResponseEntity<Void> permanentlyDelete(@PathVariable Long id) {
        if (!SecurityUtils.hasPermission("Permanent Delete FTJS")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to permanently delete FTJS records.");
        }
        archiveAndDeleteService.permanentlyDelete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Permanently delete multiple FTJS records.
     */
    @DeleteMapping("/trash/permanent/batch")
    @LogAudit(module = AuditModule.FTJS, action = "BATCH_PERMANENT_DELETE_FTJS_RECORDS", severity = Severity.CRITICAL)
    public ResponseEntity<Void> permanentlyDeleteBatch(@RequestBody List<Long> ids) {
        if (!SecurityUtils.hasPermission("Permanent Delete FTJS")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to permanently delete FTJS records.");
        }
        archiveAndDeleteService.permanentlyDeleteBatch(ids);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get monthly trend report.
     */
    @GetMapping("/trend")
    public ResponseEntity<TrendReportResponse> getTrendReport(FtjsReportFilterDto filter) {
        TrendReportResponse response = ftjsReportsService.generateAdaptiveTrendReport(filter);
        return ResponseEntity.ok(response);
    }

    /**
     * Get educational background distribution report.
     */
    @GetMapping("/education-distribution")
    public ResponseEntity<EducationDistributionResponse> getEducationDistribution(FtjsReportFilterDto filter) {
        EducationDistributionResponse response = ftjsReportsService.generateEducationDistribution(filter);
        return ResponseEntity.ok(response);
    }

    /**
     * Get purpose distribution report.
     */
    @GetMapping("/purpose-distribution")
    public ResponseEntity<PurposeDistributionResponse> getPurposeDistribution(FtjsReportFilterDto filter) {
        PurposeDistributionResponse response = ftjsReportsService.generatePurposeDistribution(filter);
        return ResponseEntity.ok(response);
    }

    /**
     * Get issuance type distribution report.
     */
    @GetMapping("/issuance-distribution")
    public ResponseEntity<IssuanceTypeDistributionResponse> getIssuanceTypeDistribution(FtjsReportFilterDto filter) {
        IssuanceTypeDistributionResponse response = ftjsReportsService.generateIssuanceTypeDistribution(filter);
        return ResponseEntity.ok(response);
    }

    /**
     * Get paginated FTJS report table.
     */
    @GetMapping("/reports/table")
    public ResponseEntity<Page<FtjsReportTableResponse>> getReportTable(
            FtjsReportFilterDto filter,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<FtjsReportTableResponse> response = ftjsReportsService.generateReportTable(filter, search, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get FTJS report KPI summary cards.
     */
    @GetMapping("/reports/kpi")
    public ResponseEntity<FtjsReportKpiResponse> getReportKpi(FtjsReportFilterDto filter) {
        FtjsReportKpiResponse response = ftjsReportsService.generateReportKpi(filter);
        return ResponseEntity.ok(response);
    }

    /**
     * Get dashboard summary statistics.
     */
    @GetMapping("/dashboard-stats")
    public ResponseEntity<FtjsDashboardSummaryDto> getSummaryStats() {
        return ResponseEntity.ok(ftjsService.getDashboardSummary());
    }
}
