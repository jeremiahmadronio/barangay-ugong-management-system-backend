package com.barangay.people.officer.controller;

import com.barangay.people.officer.service.ArchiveOfficerService;
import com.barangay.zshared.audit.annotations.LogAudit;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for managing archived and soft-deleted Barangay Officers.
 * Provides endpoints for viewing archives, restoring officers, soft delete (trash), and permanent deletion.
 */
@RestController
@RequestMapping("/api/v1/archive-officer")
@RequiredArgsConstructor
public class ArchiveOfficerController {

    private final ArchiveOfficerService archiveOfficerService;

    /**
     * Retrieve a list of archived officers matching an optional search term.
     *
     * @param search keyword matching employee name or position
     * @return response containing list of archived officers
     */
    @GetMapping("/officers")
    public ResponseEntity<List<Map<String, Object>>> getArchivedOfficers(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(archiveOfficerService.getArchivedOfficers(search));
    }

    /**
     * Restore an archived or soft-deleted officer back to ACTIVE status.
     *
     * @param id primary key identifier of the officer
     * @return success confirmation message
     */
    @PutMapping("/officers/{id}/restore")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "RESTORE_OFFICER", severity = Severity.INFO)
    public ResponseEntity<String> restoreOfficer(@PathVariable Long id) {
        archiveOfficerService.restoreOfficer(id);
        return ResponseEntity.ok("Officer restored successfully");
    }

    /**
     * Move an officer to the recycle bin (soft delete).
     *
     * @param id primary key identifier of the officer
     * @return deletion confirmation message
     */
    @DeleteMapping("/officers/{id}")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "SOFT_DELETE_OFFICER", severity = Severity.INFO)
    public ResponseEntity<String> softDeleteOfficer(@PathVariable Long id) {
        archiveOfficerService.softDeleteOfficer(id);
        return ResponseEntity.ok("Officer moved to trash");
    }

    /**
     * Permanently purge an officer record from the database.
     *
     * @param id primary key identifier of the officer
     * @return permanent deletion confirmation message
     */
    @DeleteMapping("/officers/{id}/permanent")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "DELETE_OFFICER_PERMANENT", severity = Severity.WARNING)
    public ResponseEntity<String> deleteOfficerPermanent(@PathVariable Long id) {
        archiveOfficerService.deleteOfficerPermanent(id);
        return ResponseEntity.ok("Officer deleted permanently");
    }

    /**
     * Retrieve a list of soft-deleted officers currently residing in the recycle bin.
     *
     * @param search keyword matching employee name or position
     * @return response containing list of trashed officers
     */
    @GetMapping("/trash/officers")
    public ResponseEntity<List<Map<String, Object>>> getTrashOfficers(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(archiveOfficerService.getTrashOfficers(search));
    }
}
