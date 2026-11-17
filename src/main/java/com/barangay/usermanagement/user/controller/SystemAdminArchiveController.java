package com.barangay.usermanagement.user.controller;

import com.barangay.usermanagement.user.service.SystemAdminArchiveService;
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
@RequestMapping("/api/v1/system-admin/archive")
@RequiredArgsConstructor
public class SystemAdminArchiveController {

    private final SystemAdminArchiveService archiveService;

    @GetMapping("/settings")
    public ResponseEntity<ArchiveSettingsDTO> getSettings() {
        return ResponseEntity.ok(archiveService.getSettings());
    }

    @PutMapping("/settings")
    @LogAudit(module = AuditModule.SYSTEM, action = "UPDATE_ARCHIVE_SETTINGS", severity = Severity.INFO)
    public ResponseEntity<ArchiveSettingsDTO> updateSettings(@RequestBody ArchiveSettingsDTO request) {
        return ResponseEntity.ok(archiveService.updateSettings(request));
    }

    @GetMapping("/stats")
    public ResponseEntity<ArchiveStatsDTO> getArchiveStats() {
        return ResponseEntity.ok(archiveService.getArchiveStats());
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getArchivedUsers(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(archiveService.getArchivedUsers(search));
    }

    @PutMapping("/users/{id}/restore")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "RESTORE_USER", severity = Severity.INFO)
    public ResponseEntity<String> restoreUser(@PathVariable Long id) {
        archiveService.restoreUser(id);
        return ResponseEntity.ok("User restored successfully");
    }

    @DeleteMapping("/users/{id}")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "SOFT_DELETE_USER", severity = Severity.INFO)
    public ResponseEntity<String> softDeleteUser(@PathVariable Long id) {
        archiveService.softDeleteUser(id);
        return ResponseEntity.ok("User moved to trash");
    }

    @DeleteMapping("/users/{id}/permanent")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "DELETE_USER_PERMANENT", severity = Severity.WARNING)
    public ResponseEntity<String> deleteUserPermanent(@PathVariable Long id) {
        archiveService.deleteUserPermanent(id);
        return ResponseEntity.ok("User deleted permanently");
    }

    @GetMapping("/trash/users")
    public ResponseEntity<List<Map<String, Object>>> getTrashUsers(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(archiveService.getTrashUsers(search));
    }

}
