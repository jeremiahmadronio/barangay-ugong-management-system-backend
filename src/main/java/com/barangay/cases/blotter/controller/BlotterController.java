package com.barangay.cases.blotter.controller;

import com.barangay.cases.blotter.dto.BlotterCaseCreateRequest;
import com.barangay.cases.blotter.dto.BlotterCaseResponse;
import com.barangay.cases.blotter.dto.BlotterStatsResponse;
import com.barangay.cases.blotter.service.BlotterService;
import com.barangay.zshared.audit.annotations.LogAudit;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.Severity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Blotter Case Management.
 * Supports creating, listing, searching, status updating, and deleting
 * both Formal Complaint and For the Record cases.
 * All mutating operations are logged via @LogAudit.
 */
@RestController
@RequestMapping("/api/v1/blotter")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class BlotterController {

    private final BlotterService blotterService;

    /**
     * Create a new Blotter Case entry (Formal Complaint or For the Record).
     */
    @PostMapping
    @LogAudit(
            module = AuditModule.BLOTTER,
            action = "CREATE_BLOTTER_CASE",
            severity = Severity.INFO,
            captureNewValue = true
    )
    public ResponseEntity<BlotterCaseResponse> createBlotterCase(
            @Valid @RequestBody BlotterCaseCreateRequest request
    ) {
        BlotterCaseResponse response = blotterService.createBlotterCase(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Fetch list of all Blotter Cases with search, status filtering, and optional pagination.
     */
    @GetMapping
    public ResponseEntity<?> getAllBlotterCases(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String natureOfComplaint,
            @RequestParam(required = false) String filingType,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        if (page != null && size != null) {
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
            return ResponseEntity.ok(blotterService.getBlotterCasesPage(search, status, natureOfComplaint, filingType, pageable));
        }
        return ResponseEntity.ok(blotterService.getAllBlotterCases(search, status, natureOfComplaint, filingType));
    }

    /**
     * Fetch summary statistics for Blotter dashboard.
     */
    @GetMapping("/stats")
    public ResponseEntity<BlotterStatsResponse> getBlotterStats() {
        return ResponseEntity.ok(blotterService.getBlotterStats());
    }

    /**
     * Fetch detailed info for a single Blotter Case by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BlotterCaseResponse> getBlotterCaseById(@PathVariable Long id) {
        return ResponseEntity.ok(blotterService.getBlotterCaseById(id));
    }

    /**
     * Update status of an existing Blotter Case.
     */
    @PatchMapping("/{id}/status")
    @LogAudit(
            module = AuditModule.BLOTTER,
            action = "UPDATE_BLOTTER_STATUS",
            severity = Severity.WARNING,
            captureOldValue = true,
            captureNewValue = true
    )
    public ResponseEntity<BlotterCaseResponse> updateCaseStatus(
            @PathVariable Long id,
            @RequestParam CaseStatus status,
            @RequestParam(required = false) String remarks
    ) {
        return ResponseEntity.ok(blotterService.updateCaseStatus(id, status, remarks));
    }

    /**
     * Delete (archive) a Blotter Case by ID.
     */
    @DeleteMapping("/{id}")
    @LogAudit(
            module = AuditModule.BLOTTER,
            action = "DELETE_BLOTTER_CASE",
            severity = Severity.WARNING,
            captureOldValue = true
    )
    public ResponseEntity<Void> deleteBlotterCase(@PathVariable Long id) {
        blotterService.deleteBlotterCase(id);
        return ResponseEntity.noContent().build();
    }
}
