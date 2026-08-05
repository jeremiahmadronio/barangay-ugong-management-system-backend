package com.barangay.cases.blotter.controller;

import com.barangay.cases.blotter.dto.BlotterCaseCreateRequest;
import com.barangay.cases.blotter.dto.BlotterCaseDetailResponse;
import com.barangay.cases.blotter.dto.BlotterFilterRequest;
import com.barangay.cases.blotter.dto.BlotterStatsResponse;
import com.barangay.cases.blotter.service.BlotterService;
import com.barangay.zshared.audit.annotations.LogAudit;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;
import com.barangay.zshared.mapper.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Blotter Case Management.
 * Handles listing blotter entries with server-side pagination/filtering,
 * retrieving case details, calculating KPI stats, and creating new blotter entries.
 */
@RestController
@RequestMapping("/api/v1/blotter")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class BlotterController {

    private final BlotterService blotterService;

    /**
     * Retrieves paginated and filtered blotter cases.
     * Uses server-side pagination without findAll().
     *
     * @param filter request containing search, status, filingType, natureOfComplaint, page, size
     * @return {@link PageResponse} containing the paginated list of blotter cases
     */
    @GetMapping
    public ResponseEntity<PageResponse<BlotterCaseDetailResponse>> getBlotterCases(
            @ModelAttribute BlotterFilterRequest filter) {
        PageResponse<BlotterCaseDetailResponse> response = blotterService.getBlotterCases(filter);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves KPI Statistics for Blotter cases with month-to-date vs full previous month trends.
     *
     * @return {@link BlotterStatsResponse} containing counts and trends
     */
    @GetMapping("/stats")
    public ResponseEntity<BlotterStatsResponse> getBlotterStats() {
        BlotterStatsResponse stats = blotterService.getBlotterStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Retrieves a single blotter case detail by ID.
     *
     * @param id the case ID
     * @return {@link BlotterCaseDetailResponse}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BlotterCaseDetailResponse> getBlotterCaseById(@PathVariable Long id) {
        BlotterCaseDetailResponse response = blotterService.getBlotterCaseById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new Blotter Case entry (Formal Complaint or For the Record).
     *
     * @param request the case creation request payload containing all case details and sub-entities
     * @return {@link ResponseEntity} containing the created case details with HTTP status 201 (Created)
     */
    @PostMapping
    @LogAudit(module = AuditModule.BLOTTER, action = "CREATE_BLOTTER_CASE", severity = Severity.INFO)
    public ResponseEntity<BlotterCaseDetailResponse> createBlotterCase(@Valid @RequestBody BlotterCaseCreateRequest request) {
        BlotterCaseDetailResponse response = blotterService.createBlotterCase(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
