package com.barangay.ftjs.controller;

import java.util.List;

import com.barangay.ftjs.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.web.PageableDefault;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.barangay.ftjs.service.FtjsService;
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
    private final AuditLogService auditLogService;

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
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
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
}
