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

@RestController
@RequestMapping("/api/v1/archive-officer")
@RequiredArgsConstructor
public class ArchiveOfficerController {

    private final ArchiveOfficerService archiveOfficerService;

    @GetMapping("/officers")
    public ResponseEntity<List<Map<String, Object>>> getArchivedOfficers(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(archiveOfficerService.getArchivedOfficers(search));
    }

    @PutMapping("/officers/{id}/restore")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "RESTORE_OFFICER", severity = Severity.INFO)
    public ResponseEntity<String> restoreOfficer(@PathVariable Long id) {
        archiveOfficerService.restoreOfficer(id);
        return ResponseEntity.ok("Officer restored successfully");
    }

    @DeleteMapping("/officers/{id}")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "SOFT_DELETE_OFFICER", severity = Severity.INFO)
    public ResponseEntity<String> softDeleteOfficer(@PathVariable Long id) {
        archiveOfficerService.softDeleteOfficer(id);
        return ResponseEntity.ok("Officer moved to trash");
    }

    @DeleteMapping("/officers/{id}/permanent")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "DELETE_OFFICER_PERMANENT", severity = Severity.WARNING)
    public ResponseEntity<String> deleteOfficerPermanent(@PathVariable Long id) {
        archiveOfficerService.deleteOfficerPermanent(id);
        return ResponseEntity.ok("Officer deleted permanently");
    }

    @GetMapping("/trash/officers")
    public ResponseEntity<List<Map<String, Object>>> getTrashOfficers(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(archiveOfficerService.getTrashOfficers(search));
    }
}
