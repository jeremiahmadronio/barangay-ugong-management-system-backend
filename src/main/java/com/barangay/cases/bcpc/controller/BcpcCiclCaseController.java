package com.barangay.cases.bcpc.controller;

import com.barangay.cases.bcpc.dto.BcpcCiclCaseCreateRequest;
import com.barangay.cases.bcpc.dto.BcpcCiclCaseDetailResponse;
import com.barangay.cases.bcpc.dto.BcpcCiclCaseUpdateRequest;
import com.barangay.cases.bcpc.service.BcpcCiclCaseService;
import com.barangay.zshared.audit.annotations.LogAudit;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller dedicated to Child in Conflict with the Law (CICL) case operations under BCPC.
 * Enforces legal procedures under RA 9344 / RA 10630.
 */
@RestController
@RequestMapping("/api/v1/bcpc/cicl")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class BcpcCiclCaseController {

    private final BcpcCiclCaseService bcpcCiclCaseService;

    /**
     * Files a new Child in Conflict with the Law (CICL) case.
     */
    @PostMapping
    @LogAudit(module = AuditModule.BCPC, action = "CREATE_CICL_CASE", severity = Severity.INFO)
    public ResponseEntity<BcpcCiclCaseDetailResponse> createCiclCase(
            @Valid @RequestBody BcpcCiclCaseCreateRequest request
    ) {
        BcpcCiclCaseDetailResponse response = bcpcCiclCaseService.createCiclCase(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves a single Child in Conflict with the Law (CICL) case detail by Case ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BcpcCiclCaseDetailResponse> getCiclCaseById(@PathVariable Long id) {
        BcpcCiclCaseDetailResponse response = bcpcCiclCaseService.getCiclCaseById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing Child in Conflict with the Law (CICL) case details.
     * Preserves minor and victim identities while updating incident specifics,
     * legal assessment, apprehension details, narrative, evidences, and witnesses.
     */
    @PutMapping("/{id}")
    @LogAudit(module = AuditModule.BCPC, action = "UPDATE_CICL_CASE", severity = Severity.INFO)
    public ResponseEntity<BcpcCiclCaseDetailResponse> updateCiclCase(
            @PathVariable Long id,
            @Valid @RequestBody BcpcCiclCaseUpdateRequest request
    ) {
        BcpcCiclCaseDetailResponse response = bcpcCiclCaseService.updateCiclCase(id, request);
        return ResponseEntity.ok(response);
    }
}

