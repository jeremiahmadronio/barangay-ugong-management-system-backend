package com.barangay.cases.bcpc.controller;

import com.barangay.cases.bcpc.dto.BcpcCarCaseCreateRequest;
import com.barangay.cases.bcpc.dto.BcpcCarCaseDetailResponse;
import com.barangay.cases.bcpc.dto.BcpcCiclCaseCreateRequest;
import com.barangay.cases.bcpc.dto.BcpcCiclCaseDetailResponse;
import com.barangay.cases.bcpc.service.BcpcCaseService;
import com.barangay.cases.blotter.service.CaseFileService;
import com.barangay.zshared.audit.annotations.LogAudit;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Barangay Council for the Protection of Children (BCPC) operations.
 * Provides distinct and dedicated endpoints for:
 * 1. Child at Risk (CAR) cases
 * 2. Child in Conflict with the Law (CICL) cases
 * 3. File attachments (Narrative, Evidence, Witness)
 */
@RestController
@RequestMapping("/api/v1/bcpc")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class BcpcCaseController {

    private final BcpcCaseService bcpcCaseService;
    private final CaseFileService caseFileService;

    // =========================================================================
    // Child at Risk (CAR) Endpoints
    // =========================================================================

    /**
     * Files a new Child at Risk (CAR) case record under BCPC.
     *
     * @param request the CAR intake payload
     * @return the created CAR case detail response with HTTP 201 (Created)
     */
    @PostMapping("/car")
    @LogAudit(module = AuditModule.BCPC, action = "CREATE_CAR_CASE", severity = Severity.INFO)
    public ResponseEntity<BcpcCarCaseDetailResponse> createCarCase(
            @Valid @RequestBody BcpcCarCaseCreateRequest request
    ) {
        BcpcCarCaseDetailResponse response = bcpcCaseService.createCarCase(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves a single Child at Risk (CAR) case detail by Case ID.
     *
     * @param id the case ID
     * @return the CAR case details
     */
    @GetMapping("/car/{id}")
    public ResponseEntity<BcpcCarCaseDetailResponse> getCarCaseById(@PathVariable Long id) {
        BcpcCarCaseDetailResponse response = bcpcCaseService.getCarCaseById(id);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // Child in Conflict with the Law (CICL) Endpoints
    // =========================================================================

    /**
     * Files a new Child in Conflict with the Law (CICL) case record under BCPC.
     *
     * @param request the CICL intake payload
     * @return the created CICL case detail response with HTTP 201 (Created)
     */
    @PostMapping("/cicl")
    @LogAudit(module = AuditModule.BCPC, action = "CREATE_CICL_CASE", severity = Severity.INFO)
    public ResponseEntity<BcpcCiclCaseDetailResponse> createCiclCase(
            @Valid @RequestBody BcpcCiclCaseCreateRequest request
    ) {
        BcpcCiclCaseDetailResponse response = bcpcCaseService.createCiclCase(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves a single Child in Conflict with the Law (CICL) case detail by Case ID.
     *
     * @param id the case ID
     * @return the CICL case details
     */
    @GetMapping("/cicl/{id}")
    public ResponseEntity<BcpcCiclCaseDetailResponse> getCiclCaseById(@PathVariable Long id) {
        BcpcCiclCaseDetailResponse response = bcpcCaseService.getCiclCaseById(id);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // File Download Endpoints (Narrative, Evidence, Witness)
    // =========================================================================

    /**
     * Retrieves the narrative statement attachment for a given BCPC case.
     */
    @GetMapping("/files/narrative/{caseId}")
    public ResponseEntity<byte[]> getNarrativeFile(@PathVariable Long caseId) {
        return caseFileService.getNarrativeFile(caseId);
    }

    /**
     * Retrieves the evidence file for a given BCPC evidence entry.
     */
    @GetMapping("/files/evidence/{evidenceId}")
    public ResponseEntity<byte[]> getEvidenceFile(@PathVariable Long evidenceId) {
        return caseFileService.getEvidenceFile(evidenceId);
    }

    /**
     * Retrieves the witness statement file for a given BCPC witness entry.
     */
    @GetMapping("/files/witness/{witnessId}")
    public ResponseEntity<byte[]> getWitnessFile(@PathVariable Long witnessId) {
        return caseFileService.getWitnessFile(witnessId);
    }
}
