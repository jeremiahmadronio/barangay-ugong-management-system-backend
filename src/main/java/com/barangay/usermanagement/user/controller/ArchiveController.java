package com.barangay.usermanagement.user.controller;

import com.barangay.usermanagement.user.service.ArchiveService;
import com.barangay.zshared.audit.annotations.LogAudit;
import com.barangay.zshared.dto.ArchiveSettingsDTO;
import com.barangay.zshared.dto.ArchiveStatsDTO;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/archive")
@RequiredArgsConstructor
public class ArchiveController {

    private final ArchiveService archiveService;



    // --- Officers ---
    @GetMapping("/officers")
    public ResponseEntity<List<Map<String, Object>>> getArchivedOfficers(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(archiveService.getArchivedOfficers(search));
    }

    @PutMapping("/officers/{id}/restore")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "RESTORE_OFFICER", severity = Severity.INFO)
    public ResponseEntity<String> restoreOfficer(@PathVariable Long id) {
        archiveService.restoreOfficer(id);
        return ResponseEntity.ok("Officer restored successfully");
    }

    @DeleteMapping("/officers/{id}")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "SOFT_DELETE_OFFICER", severity = Severity.INFO)
    public ResponseEntity<String> softDeleteOfficer(@PathVariable Long id) {
        archiveService.softDeleteOfficer(id);
        return ResponseEntity.ok("Officer moved to trash");
    }

    @DeleteMapping("/officers/{id}/permanent")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "DELETE_OFFICER_PERMANENT", severity = Severity.WARNING)
    public ResponseEntity<String> deleteOfficerPermanent(@PathVariable Long id) {
        archiveService.deleteOfficerPermanent(id);
        return ResponseEntity.ok("Officer deleted permanently");
    }



    @GetMapping("/trash/officers")
    public ResponseEntity<List<Map<String, Object>>> getTrashOfficers(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(archiveService.getTrashOfficers(search));
    }
}
