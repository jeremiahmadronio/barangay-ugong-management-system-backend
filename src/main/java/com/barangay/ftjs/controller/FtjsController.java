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
 * REST Controller for FTJS Resident Search, returning unified Resident Information,
 * FTJS Issuance History, and Barangay Case History.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ftjs")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class FtjsController {

    private final FtjsService ftjsService;
    private final FtjsReIssuanceService  ftjsReIssuanceService;
    private final FtjsNoteService ftjsNoteService;
    private final FtjsArchiveAndDeleteService archiveAndDeleteService;
    private final FtjsRetentionService retentionService;
    private final FtjsReportsService ftjsReportsService;


    /**
     * Search residents by name/query for FTJS verification.
     * Returns list of FtjsResidentSearchResponse objects containing demographics, issuance history, and case history.
     */
    @GetMapping("/search-resident")
    public ResponseEntity<List<FtjsResidentSearchResponse>> searchResidents(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String search) {
        String searchTerm = (query != null && !query.isBlank()) ? query : search;
        return ResponseEntity.ok(ftjsService.searchResidents(searchTerm));
    }

    /**
     * Aggregate statistics for the FTJS dashboard cards.
     */
    @GetMapping("/stats")
    public ResponseEntity<FtjsStatsResponse> getStats() {
        return ResponseEntity.ok(ftjsService.getStats());
    }

    /**
     * Paginated, filterable FTJS records for the management table.
     *
     * @param search       optional text filter (FTJS number or applicant name)
     * @param status       optional FtjsStatus filter
     * @param purpose      optional purpose/document type filter
     * @param issuanceCount optional issuance count filter
     */
    @GetMapping("/summary")
    public ResponseEntity<Page<FtjsSummaryResponse>> getSummaryTable(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) FtjsStatus status,
            @RequestParam(required = false) String purpose,
            @RequestParam(required = false) Integer issuanceCount,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ftjsService.getSummaryTable(search, status, purpose, issuanceCount, pageable));
    }

    /**
     * Get FTJS resident response profile by resident ID.
     */
    @GetMapping("/resident/{residentId}")
    public ResponseEntity<FtjsResidentSearchResponse> getResidentFtjsProfile(@PathVariable Long residentId) {
        return ResponseEntity.ok(ftjsService.getResidentFtjsProfile(residentId));
    }

    /**
     * Create and save a new FTJS entry into DB with byte[] files and write Audit Logs.
     */
    @PostMapping("/entry")
    @LogAudit(module = AuditModule.FTJS, action = "CREATE_FTJS_ENTRY", severity = Severity.INFO)
    public ResponseEntity<FtjsResidentSearchResponse> createFtjsEntry(@RequestBody FtjsRequestEntry request) {
        log.info("Received FTJS application entry creation request for Resident ID: {}", request.residentId());
        FtjsResidentSearchResponse response = ftjsService.createFtjsEntry(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<FtjsDetailResponse> getFtjsDetail( @PathVariable Long id) {
        FtjsDetailResponse ftjs = ftjsService.getFtjsApplicantDetail(id);
        return ResponseEntity.ok(ftjs);

    }

    @PostMapping("re-issuance-request/{ftjsId}")
    @LogAudit(module = AuditModule.FTJS, action = "CREATE_RE-ISSUANCE_REQUEST", severity = Severity.INFO)
    public ResponseEntity<ReIssuanceRequestDto> createReIssuanceRequest(@PathVariable Long ftjsId, @RequestBody ReIssuanceRequestDto request) throws BadRequestException {
        ReIssuanceRequestDto reIssuanceReq = ftjsReIssuanceService.createReIssuanceRequest(ftjsId, request);
        return ResponseEntity.ok(reIssuanceReq);
    }


    @GetMapping("/{ftjsId}/re-issuances/history")
    public ResponseEntity<List<ReIssuanceHistoryDto>> getReIssuanceHistory(@PathVariable Long ftjsId) {
        return ResponseEntity.ok(ftjsReIssuanceService.getReIssuanceHistory(ftjsId));
    }

    @GetMapping("/{ftjsId}/timeline/")
    public ResponseEntity<List<FtjsTimelineDto>> getTimeline(@PathVariable Long ftjsId) {
        return ResponseEntity.ok(ftjsReIssuanceService.getFtjsTimeline(ftjsId));
    }


    @PostMapping("/notes-entry/{ftjsId}")
    public ResponseEntity<FtjsNoteResponse> addNote(@PathVariable Long ftjsId, @Valid @RequestBody FtjsNoteRequest request) {
        FtjsNoteResponse response = ftjsNoteService.addNote(ftjsId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping("/notes-history/{ftjsId}")
    public ResponseEntity<List<FtjsNoteResponse>> getNotes(@PathVariable Long ftjsId) {
        List<FtjsNoteResponse> notes = ftjsNoteService.getNotesByFtjsId(ftjsId);
        return ResponseEntity.ok(notes);
    }

    @PostMapping("/archive/{id}")
    public ResponseEntity<Void> archiveFtjs(@PathVariable Long id){
        archiveAndDeleteService.archiveFtjs(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/restore/{id}")
    public ResponseEntity<Void> restoreFtjs(@PathVariable Long id){
        archiveAndDeleteService.restoreFtjs(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/archive/stats")
    public ResponseEntity<FtjsArchiveStatsResponse> getArchiveAndDeleteStats(){
        FtjsArchiveStatsResponse stats = archiveAndDeleteService.getArchiveStats();
        return ResponseEntity.ok(stats);
    }


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

    @GetMapping("/display-retention")
    public ResponseEntity<RetentionSettingDto.Response> getRetentionSetting() {
        return ResponseEntity.ok(retentionService.getCurrentRetentionSetting());
    }

    @PutMapping("/update-retention")
    public ResponseEntity<Void> updateRetentionSetting(@Valid @RequestBody RetentionSettingDto.Request request) {
        retentionService.updateRetentionSetting(request);
        return ResponseEntity.noContent().build();
    }




    @DeleteMapping("/{id}")
    @LogAudit(module = AuditModule.FTJS, action = "SOFT_DELETE_FTJS_RECORD", severity = Severity.LOW)
    public ResponseEntity<Void> softDelete(@PathVariable Long id) {
        archiveAndDeleteService.softDelete(id);
        return ResponseEntity.noContent().build();
    }


    @DeleteMapping("/trash/{id}/permanent")
    @LogAudit(module = AuditModule.FTJS, action = "PERMANENT_DELETE_FTJS_RECORD", severity = Severity.WARNING)
    public ResponseEntity<Void> permanentlyDelete(@PathVariable Long id) {
        archiveAndDeleteService.permanentlyDelete(id);
        return ResponseEntity.noContent().build();
    }


    @DeleteMapping("/trash/permanent/batch")
    @LogAudit(module = AuditModule.FTJS, action = "BATCH_PERMANENT_DELETE_FTJS_RECORDS", severity = Severity.CRITICAL)
    public ResponseEntity<Void> permanentlyDeleteBatch(@RequestBody List<Long> ids) {
        archiveAndDeleteService.permanentlyDeleteBatch(ids);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/trend")
    public ResponseEntity<TrendReportResponse> getTrendReport(FtjsReportFilterDto filter) {
        TrendReportResponse response = ftjsReportsService.generateAdaptiveTrendReport(filter);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/education-distribution")
    public ResponseEntity<EducationDistributionResponse> getEducationDistribution(FtjsReportFilterDto filter) {
        EducationDistributionResponse response = ftjsReportsService.generateEducationDistribution(filter);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/purpose-distribution")
    public ResponseEntity<PurposeDistributionResponse> getPurposeDistribution(FtjsReportFilterDto filter) {
        PurposeDistributionResponse response = ftjsReportsService.generatePurposeDistribution(filter);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/issuance-distribution")
    public ResponseEntity<IssuanceTypeDistributionResponse> getIssuanceTypeDistribution(FtjsReportFilterDto filter) {
        IssuanceTypeDistributionResponse response = ftjsReportsService.generateIssuanceTypeDistribution(filter);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reports/table")
    public ResponseEntity<Page<FtjsReportTableResponse>> getReportTable(
            FtjsReportFilterDto filter,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<FtjsReportTableResponse> response = ftjsReportsService.generateReportTable(filter, search, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reports/kpi")
    public ResponseEntity<FtjsReportKpiResponse> getReportKpi(FtjsReportFilterDto filter) {
        FtjsReportKpiResponse response = ftjsReportsService.generateReportKpi(filter);
        return ResponseEntity.ok(response);
    }
}
