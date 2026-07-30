package com.barangay.people.resident.controller;

import com.barangay.people.resident.dto.*;
import com.barangay.people.resident.service.ResidentService;
import com.barangay.zshared.audit.annotations.LogAudit;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.ResidentStatus;
import com.barangay.zshared.enumerated.Severity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller exposing endpoints for Resident Management.
 * Covers dashboard statistics, filtered table queries,
 * detail profile viewing, CRUD, and status updates.
 * All mutating operations are automatically captured by the @LogAudit aspect.
 */
@RestController
@RequestMapping("/api/v1/residents")
@RequiredArgsConstructor
public class ResidentController {

    private final ResidentService residentService;

    /**
     * Fetch Resident Management Dashboard statistics with Month-over-Month trend percentages.
     */
    @GetMapping("/stats")
    public ResponseEntity<ResidentManagementStatsResponse> getResidentStats() {
        return ResponseEntity.ok(residentService.getResidentStats());
    }

    /**
     * Fetch Resident table list with filtering by search (Name or Barangay ID), status, gender, and age group.
     */
    @GetMapping("/table")
    public ResponseEntity<List<ResidentTableResponse>> getResidentsTable(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String ageGroup
    ) {
        ResidentFilterRequest filter = new ResidentFilterRequest(search, status, gender, ageGroup);
        return ResponseEntity.ok(residentService.getFilteredResidentsTable(filter));
    }

    /**
     * Fetch detailed Resident Profile for viewing.
     */
    @GetMapping("/{id}/details")
    public ResponseEntity<ResidentDetailResponse> getResidentDetail(@PathVariable Long id) {
        return ResponseEntity.ok(residentService.getResidentDetail(id));
    }

    /**
     * Suggests the next Barangay ID number and Household Number for new resident forms.
     * Called when the Create form opens to pre-populate unique sequential values.
     */
    @GetMapping("/suggestions")
    public ResponseEntity<ResidentSuggestionsResponse> getSuggestions() {
        return ResponseEntity.ok(residentService.getSuggestions());
    }

    /**
     * Search active residents (status = ACTIVE) for officer registration or select inputs.
     */
    @GetMapping("/search-active")
    public ResponseEntity<List<ResidentSearchOptionResponse>> searchActiveResidents(
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(residentService.searchActiveResidents(search));
    }

    /**
     * Returns all active residents sharing the given last name.
     * Used for the family/relative suggestion panel that appears when typing a last name in the form.
     */
    @GetMapping("/relatives")
    public ResponseEntity<List<ResidentRelativeResponse>> getRelativesByLastName(
            @RequestParam String lastName
    ) {
        return ResponseEntity.ok(residentService.getRelativesByLastName(lastName));
    }

    /**
     * Register a new Resident.
     * Audit: captures the created resident record as new value.
     */
    @PostMapping
    @LogAudit(
            module = AuditModule.RESIDENTS,
            action = "CREATE_RESIDENT",
            severity = Severity.INFO,
            captureNewValue = true
    )
    public ResponseEntity<ResidentTableResponse> createResident(
            @Valid @RequestBody ResidentCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(residentService.createResident(request));
    }

    /**
     * Update existing Resident information.
     * Audit: captures the request as old value and updated resident as new value.
     */
    @PutMapping("/{id}")
    @LogAudit(
            module = AuditModule.RESIDENTS,
            action = "UPDATE_RESIDENT",
            severity = Severity.INFO,
            captureOldValue = true,
            captureNewValue = true
    )
    public ResponseEntity<ResidentTableResponse> updateResident(
            @PathVariable Long id,
            @Valid @RequestBody ResidentUpdateRequest request
    ) {
        return ResponseEntity.ok(residentService.updateResident(id, request));
    }

    /**
     * Update a Resident's status (ACTIVE, INACTIVE, DECEASED, MOVED_OUT).
     * Audit: severity WARNING because status change is a sensitive action.
     */
    @PatchMapping("/{id}/status")
    @LogAudit(
            module = AuditModule.RESIDENTS,
            action = "UPDATE_RESIDENT_STATUS",
            severity = Severity.INFO,
            captureOldValue = true,
            captureNewValue = true
    )
    public ResponseEntity<ResidentTableResponse> updateResidentStatus(
            @PathVariable Long id,
            @Valid @RequestBody ResidentStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(residentService.updateResidentStatus(id, request));
    }

    /**
     * Archive a Resident record.
     */
    @PatchMapping("/{id}/archive")
    @LogAudit(
            module = AuditModule.RESIDENTS,
            action = "ARCHIVE_RESIDENT",
            severity = Severity.INFO,
            captureOldValue = true,
            captureNewValue = true
    )
    public ResponseEntity<ResidentTableResponse> archiveResident(@PathVariable Long id) {
        return ResponseEntity.ok(residentService.archiveResident(id));
    }

    /**
     * Restore an archived Resident record back to active.
     */
    @PatchMapping("/{id}/restore")
    @LogAudit(
            module = AuditModule.RESIDENTS,
            action = "RESTORE_RESIDENT",
            severity = Severity.INFO,
            captureOldValue = true,
            captureNewValue = true
    )
    public ResponseEntity<ResidentTableResponse> restoreResident(@PathVariable Long id) {
        return ResponseEntity.ok(residentService.restoreResident(id));
    }

    /**
     * Delete / Inactivate a Resident record.
     * Audit: severity HIGH because deletion is irreversible.
     */
    @DeleteMapping("/{id}")
    @LogAudit(
            module = AuditModule.RESIDENTS,
            action = "DELETE_RESIDENT",
            severity = Severity.CRITICAL
    )
    public ResponseEntity<Void> deleteResident(@PathVariable Long id) {
        residentService.deleteResident(id);
        return ResponseEntity.noContent().build();
    }
}
