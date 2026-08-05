package com.barangay.cases.blotter.controller;

import com.barangay.cases.blotter.dto.BlotterCaseCreateRequest;
import com.barangay.cases.blotter.dto.BlotterCaseDetailResponse;
import com.barangay.cases.blotter.service.BlotterService;
import com.barangay.zshared.audit.annotations.LogAudit;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Blotter Case Management.
 * Handles creating blotter entries (Formal Complaint or For the Record).
 * All mutating operations are logged via {@link LogAudit}.
 */
@RestController
@RequestMapping("/api/v1/blotter")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class BlotterController {

    private final BlotterService blotterService;

    /**
     * Creates a new Blotter Case entry (Formal Complaint or For the Record).
     * <p>
     * This endpoint receives case metadata, complainant info, respondent info, incident details,
     * narrative statement, and optional witnesses. It saves the involved persons, builds the
     * case record, and logs an audit entry.
     * </p>
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

