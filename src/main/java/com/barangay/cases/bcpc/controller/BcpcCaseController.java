package com.barangay.cases.bcpc.controller;

import com.barangay.cases.bcpc.dto.BcpcCarCaseCreateRequest;
import com.barangay.cases.bcpc.dto.BcpcCarCaseDetailResponse;
import com.barangay.cases.bcpc.dto.BcpcCiclCaseCreateRequest;
import com.barangay.cases.bcpc.dto.BcpcCiclCaseDetailResponse;
import com.barangay.cases.bcpc.dto.BcpcCustodyLogDto;
import com.barangay.cases.bcpc.dto.BcpcFilterRequest;
import com.barangay.cases.bcpc.dto.BcpcStatsResponse;
import com.barangay.cases.bcpc.dto.BcpcTableRowResponse;
import com.barangay.cases.bcpc.dto.BcpcCaseUniversalDetailResponse;
import com.barangay.cases.bcpc.dto.UpdateCiclCustodyLogDto;
import com.barangay.cases.bcpc.service.BcpcCaseService;
import com.barangay.cases.blotter.dto.CaseNoteRequest;
import com.barangay.cases.blotter.dto.CaseNoteResponse;
import com.barangay.cases.blotter.dto.TimelineEventResponse;
import com.barangay.cases.blotter.service.CaseFileService;
import com.barangay.cases.shared.service.CaseTimelineService;
import com.barangay.zshared.audit.annotations.LogAudit;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;
import com.barangay.zshared.mapper.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    private final CaseTimelineService caseTimelineService;

    /**
     * Retrieves BCPC KPI statistics (Total cases with month trend, active, referred, settled).
     * Excludes permanently deleted, deleted (trash), and archived cases.
     *
     * @return the BCPC KPI statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<BcpcStatsResponse> getStats() {
        BcpcStatsResponse response = bcpcCaseService.getBcpcStats();
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves paginated and filtered BCPC case table rows.
     * Page size defaults to 5.
     *
     * @param filter the filtering criteria (search, status, category, page, size)
     * @return {@link PageResponse} of {@link BcpcTableRowResponse}
     */
    @GetMapping("/table")
    public ResponseEntity<PageResponse<BcpcTableRowResponse>> getBcpcTable(
            @ModelAttribute BcpcFilterRequest filter
    ) {
        PageResponse<BcpcTableRowResponse> response = bcpcCaseService.getBcpcCasesTable(filter);
        return ResponseEntity.ok(response);
    }


    /**
     * Retrieves complete universal case details by ID (automatically detects CAR or CICL).
     */
    @GetMapping("/{id}")
    public ResponseEntity<BcpcCaseUniversalDetailResponse> getCaseDetailsById(@PathVariable Long id) {
        BcpcCaseUniversalDetailResponse response = bcpcCaseService.getCaseDetailsById(id);
        return ResponseEntity.ok(response);
    }

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

    /**
     * Retrieves all timeline events for a given BCPC case.
     */
    @GetMapping("/{caseId}/timeline")
    public ResponseEntity<java.util.List<TimelineEventResponse>> getCaseTimeline(@PathVariable Long caseId) {
        return ResponseEntity.ok(caseTimelineService.getTimelineForCase(caseId));
    }

    /**
     * Retrieves all confidential case notes for a given BCPC case.
     */
    @GetMapping("/{caseId}/notes")
    public ResponseEntity<java.util.List<CaseNoteResponse>> getCaseNotes(@PathVariable Long caseId) {
        return ResponseEntity.ok(bcpcCaseService.getCaseNotes(caseId));
    }

    /**
     * Adds a confidential case note to a given BCPC case.
     */
    @PostMapping("/{caseId}/notes")
    @LogAudit(module = AuditModule.BCPC, action = "ADD_CASE_NOTE", severity = Severity.INFO)
    public ResponseEntity<CaseNoteResponse> addCaseNote(
            @PathVariable Long caseId,
            @Valid @RequestBody CaseNoteRequest request
    ) {
        CaseNoteResponse response = bcpcCaseService.addCaseNote(caseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves the Custody & Turnover record for a BCPC case.
     */
    @GetMapping("/{caseId}/custody-log")
    public ResponseEntity<BcpcCustodyLogDto> getCustodyLog(@PathVariable Long caseId) {
        return ResponseEntity.ok(bcpcCaseService.getCustodyLog(caseId));
    }

    /**
     * Records or updates a Custody & Turnover entry for a BCPC case (primarily CAR placement).
     */
    @PutMapping("/{caseId}/custody-log")
    @LogAudit(module = AuditModule.BCPC, action = "BCPC_CUSTODY_TURNOVER_RECORDED", severity = Severity.INFO)
    public ResponseEntity<BcpcCustodyLogDto> updateCustodyLog(
            @PathVariable Long caseId,
            @RequestBody BcpcCustodyLogDto request
    ) {
        BcpcCustodyLogDto response = bcpcCaseService.updateCustodyLog(caseId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Records or updates a CICL Custody / Apprehension log.
     */
    @PutMapping("/{caseId}/cicl-custody-log")
    @LogAudit(module = AuditModule.BCPC, action = "CICL_CUSTODY_RECORDED", severity = Severity.INFO)
    public ResponseEntity<BcpcCustodyLogDto> updateCiclCustodyLog(
            @PathVariable Long caseId,
            @RequestBody UpdateCiclCustodyLogDto request
    ) {
        BcpcCustodyLogDto response = bcpcCaseService.updateCiclCustodyLog(caseId, request);
        return ResponseEntity.ok(response);
    }
}
