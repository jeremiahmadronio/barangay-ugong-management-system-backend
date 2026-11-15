package com.barangay.clearance.controller;

import com.barangay.clearance.dto.ArchiveClearanceRequest;
import com.barangay.clearance.dto.ClearanceArchiveItemResponse;
import com.barangay.clearance.dto.ClearanceLifecycleKpiResponse;
import com.barangay.clearance.dto.ClearanceRecentDeleteItemResponse;
import com.barangay.clearance.service.ClearanceArchive;
import com.barangay.zshared.audit.annotations.LogAudit;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;
import com.barangay.zshared.mapper.PageResponse;
import com.barangay.zshared.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

/**
 * Controller exposing REST endpoints for Clearance Archive and Trash Vault management.
 */
@RestController
@RequestMapping("/api/v1/clearance/archive")
@RequiredArgsConstructor
public class ClearanceArchiveController {

    private final ClearanceArchive clearanceArchive;

    /**
     * Endpoint to fetch the 4 lifecycle KPI metrics for Clearance Archive and Trash Vault.
     */
    @GetMapping({"/kpi", "/stats"})
    public ResponseEntity<ClearanceLifecycleKpiResponse> getClearanceLifecycleKpi() {
        return ResponseEntity.ok(clearanceArchive.getClearanceLifecycleKpi());
    }

    /**
     * Endpoint to retrieve paginated archived certificates.
     */
    @GetMapping("/archived")
    public ResponseEntity<PageResponse<ClearanceArchiveItemResponse>> getArchivedCertificates(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(clearanceArchive.getArchivedCertificates(search, page, size));
    }

    /**
     * Endpoint to retrieve paginated soft-deleted certificates in the recycle bin.
     */
    @GetMapping("/trash")
    public ResponseEntity<PageResponse<ClearanceRecentDeleteItemResponse>> getRecentDeletedCertificates(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(clearanceArchive.getRecentDeletedCertificates(search, page, size));
    }

    /**
     * Archives an active issued certificate.
     */
    @PostMapping("/{id}/archive")
    @LogAudit(module = AuditModule.CLEARANCE, action = "ARCHIVE_CLEARANCE", severity = Severity.INFO)
    public ResponseEntity<Void> archiveCertificate(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ArchiveClearanceRequest request
    ) {
        if (!SecurityUtils.hasPermission("Archive Clearance")) {
            throw new AccessDeniedException("You do not have permission to archive clearances.");
        }
        clearanceArchive.archiveCertificate(id, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Restores an archived certificate back to active records.
     */
    @PostMapping("/{id}/restore")
    @LogAudit(module = AuditModule.CLEARANCE, action = "RESTORE_ARCHIVED_CLEARANCE", severity = Severity.INFO)
    public ResponseEntity<Void> restoreArchivedCertificate(@PathVariable Long id) {
        if (!SecurityUtils.hasPermission("Restore Clearance")) {
            throw new AccessDeniedException("You do not have permission to restore clearances.");
        }
        clearanceArchive.restoreArchivedCertificate(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Soft-deletes a certificate (moves to trash vault).
     */
    @PostMapping("/{id}/soft-delete")
    @LogAudit(module = AuditModule.CLEARANCE, action = "SOFT_DELETE_CLEARANCE", severity = Severity.WARNING)
    public ResponseEntity<Void> softDeleteCertificate(@PathVariable Long id) {
        if (!SecurityUtils.hasPermission("Delete Clearance")) {
            throw new AccessDeniedException("You do not have permission to delete clearances.");
        }
        clearanceArchive.softDelete(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Restores a soft-deleted certificate from the trash vault.
     */
    @PostMapping("/{id}/restore-trash")
    @LogAudit(module = AuditModule.CLEARANCE, action = "RESTORE_TRASH_CLEARANCE", severity = Severity.INFO)
    public ResponseEntity<Void> restoreDeletedCertificate(@PathVariable Long id) {
        if (!SecurityUtils.hasPermission("Restore Clearance")) {
            throw new AccessDeniedException("You do not have permission to restore clearances.");
        }
        clearanceArchive.restoreDeletedCertificate(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Permanently purges a certificate in trash.
     */
    @DeleteMapping("/{id}/permanent")
    @LogAudit(module = AuditModule.CLEARANCE, action = "PERMANENT_DELETE_CLEARANCE", severity = Severity.CRITICAL)
    public ResponseEntity<Void> permanentDeleteCertificate(@PathVariable Long id) {
        if (!SecurityUtils.hasPermission("Permanent Delete Clearance")) {
            throw new AccessDeniedException("You do not have permission to permanently delete clearances.");
        }
        clearanceArchive.permanentDeleteCertificate(id);
        return ResponseEntity.ok().build();
    }
}
