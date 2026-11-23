package com.barangay.cases.bcpc.controller;

import com.barangay.cases.bcpc.dto.BcpcCarCaseCreateRequest;
import com.barangay.cases.bcpc.dto.BcpcCarCaseDetailResponse;
import com.barangay.cases.bcpc.dto.BcpcCarCaseUpdateRequest;
import com.barangay.cases.bcpc.service.BcpcCarCaseService;
import com.barangay.zshared.audit.annotations.LogAudit;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;
import com.barangay.zshared.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller dedicated to Child at Risk (CAR) case operations under BCPC.
 * Separated cleanly from CICL to enforce distinct statutory guidelines and validations.
 */
@RestController
@RequestMapping("/api/v1/bcpc/car")
@RequiredArgsConstructor
public class BcpcCarCaseController {

    private final BcpcCarCaseService bcpcCarCaseService;

    /**
     * Files a new Child at Risk (CAR) case record.
     */
    @PostMapping
    @LogAudit(module = AuditModule.BCPC, action = "CREATE_CAR_CASE", severity = Severity.INFO)
    public ResponseEntity<BcpcCarCaseDetailResponse> createCarCase(
            @Valid @RequestBody BcpcCarCaseCreateRequest request
    ) {
        if (!SecurityUtils.hasPermission("File Complaint")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to file complaints.");
        }
        BcpcCarCaseDetailResponse response = bcpcCarCaseService.createCarCase(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves a single Child at Risk (CAR) case detail by Case ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BcpcCarCaseDetailResponse> getCarCaseById(@PathVariable Long id) {
        if (!SecurityUtils.hasPermission("View Cases")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to view cases.");
        }
        BcpcCarCaseDetailResponse response = bcpcCarCaseService.getCarCaseById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing Child at Risk (CAR) case details.
     * Preserves child and reporter identities while updating incident specifics,
     * guardian status, CAR assessment, narrative, evidences, and witnesses.
     */
    @PutMapping("/{id}")
    @LogAudit(module = AuditModule.BCPC, action = "UPDATE_CAR_CASE", severity = Severity.INFO)
    public ResponseEntity<BcpcCarCaseDetailResponse> updateCarCase(
            @PathVariable Long id,
            @Valid @RequestBody BcpcCarCaseUpdateRequest request
    ) {
        if (!SecurityUtils.hasPermission("Edit Case")) {
            throw new AccessDeniedException("Access Denied: You do not have permission to edit cases.");
        }
        BcpcCarCaseDetailResponse response = bcpcCarCaseService.updateCarCase(id, request);
        return ResponseEntity.ok(response);
    }
}

