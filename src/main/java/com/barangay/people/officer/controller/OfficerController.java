package com.barangay.people.officer.controller;

import com.barangay.people.officer.dto.*;
import com.barangay.people.officer.service.OfficerService;
import com.barangay.zshared.audit.annotations.LogAudit;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.OfficerStatus;
import com.barangay.zshared.enumerated.Severity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller exposing endpoints for Officer Directory and Management.
 * Includes dashboard stats with MoM trend (on Total Officers only), filtered queries, officer registration, updates, and status management.
 */
@RestController
@RequestMapping("/api/v1/officers")
@RequiredArgsConstructor
public class OfficerController {

    private final OfficerService officerService;

    /**
     * Fetch Officer Management Dashboard statistics with Month-over-Month trend for total officers.
     */
    @GetMapping("/stats")
    public ResponseEntity<OfficerStatsResponse> getOfficerStats() {
        return ResponseEntity.ok(officerService.getOfficerStats());
    }

    /**
     * Fetch Officer table list with search and filtering by department and status.
     */
    @GetMapping("/table")
    public ResponseEntity<List<OfficerTableResponse>> getOfficersTable(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) OfficerStatus status
    ) {
        OfficerFilterRequest filter = new OfficerFilterRequest(search, department, status);
        return ResponseEntity.ok(officerService.getFilteredOfficersTable(filter));
    }

    /**
     * Create / Register a new Officer.
     */
    @PostMapping
    @LogAudit(
            module = AuditModule.USER_MANAGEMENT,
            action = "CREATE_OFFICER",
            severity = Severity.INFO,
            captureNewValue = true
    )
    public ResponseEntity<OfficerTableResponse> createOfficer(
            @Valid @RequestBody OfficerCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(officerService.createOfficer(request));
    }

    /**
     * Update an existing Officer record.
     */
    @PutMapping("/{id}")
    @LogAudit(
            module = AuditModule.USER_MANAGEMENT,
            action = "UPDATE_OFFICER",
            severity = Severity.INFO,
            captureOldValue = true,
            captureNewValue = true
    )
    public ResponseEntity<OfficerTableResponse> updateOfficer(
            @PathVariable Long id,
            @Valid @RequestBody OfficerUpdateRequest request
    ) {
        return ResponseEntity.ok(officerService.updateOfficer(id, request));
    }

    /**
     * Update Officer status (ACTIVE / INACTIVE).
     */
    @PatchMapping("/{id}/status")
    @LogAudit(
            module = AuditModule.USER_MANAGEMENT,
            action = "UPDATE_OFFICER_STATUS",
            severity = Severity.INFO,
            captureOldValue = true,
            captureNewValue = true
    )
    public ResponseEntity<OfficerTableResponse> updateOfficerStatus(
            @PathVariable Long id,
            @Valid @RequestBody OfficerStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(officerService.updateOfficerStatus(id, request));
    }

    /**
     * Archive an Officer record.
     */
    @PatchMapping("/{id}/archive")
    @LogAudit(
            module = AuditModule.USER_MANAGEMENT,
            action = "ARCHIVE_OFFICER",
            severity = Severity.INFO
    )
    public ResponseEntity<OfficerTableResponse> archiveOfficer(@PathVariable Long id) {
        OfficerStatusUpdateRequest req = new OfficerStatusUpdateRequest(OfficerStatus.ARCHIVED);
        return ResponseEntity.ok(officerService.updateOfficerStatus(id, req));
    }

    /**
     * Restore an Officer record.
     */
    @PatchMapping("/{id}/restore")
    @LogAudit(
            module = AuditModule.USER_MANAGEMENT,
            action = "RESTORE_OFFICER",
            severity = Severity.INFO
    )
    public ResponseEntity<OfficerTableResponse> restoreOfficer(@PathVariable Long id) {
        OfficerStatusUpdateRequest req = new OfficerStatusUpdateRequest(OfficerStatus.ACTIVE);
        return ResponseEntity.ok(officerService.updateOfficerStatus(id, req));
    }

    /**
     * Delete an Officer record.
     */
    @DeleteMapping("/{id}")
    @LogAudit(
            module = AuditModule.USER_MANAGEMENT,
            action = "DELETE_OFFICER",
            severity = Severity.WARNING
    )
    public ResponseEntity<Void> deleteOfficer(@PathVariable Long id) {
        officerService.deleteOfficer(id);
        return ResponseEntity.noContent().build();
    }
}
