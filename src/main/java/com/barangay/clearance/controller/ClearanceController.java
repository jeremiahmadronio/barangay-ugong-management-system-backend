package com.barangay.clearance.controller;

import com.barangay.clearance.dto.ClearanceResidentSearchResponse;
import com.barangay.clearance.dto.ClearanceStatsResponse;
import com.barangay.clearance.dto.IssueClearanceRequest;
import com.barangay.clearance.dto.IssueClearanceResponse;
import com.barangay.clearance.dto.IssuedCertificateDetailResponse;
import com.barangay.clearance.dto.IssuedCertificateTableResponse;
import com.barangay.clearance.service.ClearanceService;
import com.barangay.clearance.service.ClearanceTemplateService;
import com.barangay.zshared.audit.annotations.LogAudit;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;
import com.barangay.zshared.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller exposing endpoints for clearance issuance, resident verification, and stats inspection.
 */
@RestController
@RequestMapping("/api/v1/clearance")
@RequiredArgsConstructor
public class ClearanceController {

    private final ClearanceTemplateService clearanceTemplateService;
    private final ClearanceService clearanceService;

    /**
     * Endpoint to search active residents for clearance issuance, returning demographic info,
     * previous clearance issuance history, and multi-department case history (BCPC, VAWC, Lupon, Blotter).
     *
     * @param query Name or Barangay ID search string
     * @return 200 OK with list of {@link ClearanceResidentSearchResponse}
     */
    @GetMapping("/search-resident")
    public ResponseEntity<List<ClearanceResidentSearchResponse>> searchResidents(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(clearanceTemplateService.searchResidents(query));
    }

    /**
     * Endpoint to fetch a single resident's unified clearance profile by Resident ID.
     *
     * @param residentId Resident ID
     * @return 200 OK with {@link ClearanceResidentSearchResponse}
     */
    @GetMapping("/resident/{residentId}")
    public ResponseEntity<ClearanceResidentSearchResponse> getResidentClearanceProfile(@PathVariable Long residentId) {
        return ResponseEntity.ok(clearanceTemplateService.getResidentClearanceProfile(residentId));
    }

    /**
     * Endpoint to fetch dashboard KPI statistics for clearance issuance.
     *
     * @return 200 OK with {@link ClearanceStatsResponse}
     */
    @GetMapping({"/stats", "/template/stats"})
    public ResponseEntity<ClearanceStatsResponse> getClearanceStats() {
        return ResponseEntity.ok(clearanceService.getClearanceStats());
    }

    /**
     * Endpoint to fetch filtered issued certificates list for table management.
     *
     * @param search Search query (clearance number or name prefix)
     * @param typeOfCert Certificate template title filter
     * @param purpose Application purpose filter
     * @return 200 OK with List of {@link IssuedCertificateTableResponse}
     */
    @GetMapping({"/issued-certificates", "/template/issued-certificates"})
    public ResponseEntity<List<IssuedCertificateTableResponse>> getIssuedCertificates(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String typeOfCert,
            @RequestParam(required = false) String purpose
    ) {
        if (!SecurityUtils.hasPermission("View Clearance")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to view clearance records.");
        }
        return ResponseEntity.ok(clearanceService.getIssuedCertificates(search, typeOfCert, purpose));
    }

    /**
     * Endpoint to fetch full, exhaustive details of a single issued certificate by ID.
     *
     * @param id Issued certificate ID
     * @return 200 OK with {@link com.barangay.clearance.dto.IssuedCertificateDetailResponse}
     */
    @GetMapping({"/issued-certificates/{id}", "/{id}"})
    public ResponseEntity<IssuedCertificateDetailResponse> getIssuedCertificateDetail(@PathVariable Long id) {
        if (!SecurityUtils.hasPermission("View Clearance")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to view clearance details.");
        }
        return ResponseEntity.ok(clearanceService.getIssuedCertificateDetail(id));
    }

    /**
     * Issues a new certificate or clearance, logs audit entry, and saves the record.
     *
     * @param request {@link IssueClearanceRequest}
     * @return 200 OK with {@link IssueClearanceResponse}
     */
    @PostMapping({"/issue-clearance", "/template/issue-clearance"})
    @LogAudit(module = AuditModule.CLEARANCE, action = "ISSUE_CLEARANCE", severity = Severity.INFO)
    public ResponseEntity<IssueClearanceResponse> issueClearance(@Valid @RequestBody IssueClearanceRequest request) {
        if (!SecurityUtils.hasPermission("Issue Clearance")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to issue clearances.");
        }
        return ResponseEntity.ok(clearanceTemplateService.issueClearance(request));
    }
}
