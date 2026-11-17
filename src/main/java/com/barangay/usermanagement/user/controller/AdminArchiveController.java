package com.barangay.usermanagement.user.controller;

import com.barangay.usermanagement.user.service.AdminArchiveService;
import com.barangay.zshared.audit.annotations.LogAudit;
import com.barangay.zshared.dto.ArchiveStatsDTO;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller exposing endpoints for Barangay Admin Archive Vault.
 * Unlike Root Admin Archive, settings policy editing is omitted and
 * only System Users (Officers/Staff) are listed and counted in user archive metrics.
 */
@RestController
@RequestMapping("/api/v1/admin/archive")
@RequiredArgsConstructor
public class AdminArchiveController {

    private final AdminArchiveService adminArchiveService;

    @GetMapping("/stats")
    public ResponseEntity<ArchiveStatsDTO> getArchiveStats() {
        return ResponseEntity.ok(adminArchiveService.getArchiveStats());
    }

    @GetMapping("/residents")
    public ResponseEntity<List<Map<String, Object>>> getArchivedResidents(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(adminArchiveService.getArchivedResidents(search));
    }

    @PutMapping("/residents/{id}/restore")
    @LogAudit(module = AuditModule.RESIDENTS, action = "RESTORE_RESIDENT", severity = Severity.INFO)
    public ResponseEntity<String> restoreResident(@PathVariable Long id) {
        adminArchiveService.restoreResident(id);
        return ResponseEntity.ok("Resident restored successfully");
    }

    @DeleteMapping("/residents/{id}")
    @LogAudit(module = AuditModule.RESIDENTS, action = "SOFT_DELETE_RESIDENT", severity = Severity.INFO)
    public ResponseEntity<String> softDeleteResident(@PathVariable Long id) {
        adminArchiveService.softDeleteResident(id);
        return ResponseEntity.ok("Resident moved to trash");
    }

    @DeleteMapping("/residents/{id}/permanent")
    @LogAudit(module = AuditModule.RESIDENTS, action = "DELETE_RESIDENT_PERMANENT", severity = Severity.WARNING)
    public ResponseEntity<String> deleteResidentPermanent(@PathVariable Long id) {
        adminArchiveService.deleteResidentPermanent(id);
        return ResponseEntity.ok("Resident deleted permanently");
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getArchivedUsers(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(adminArchiveService.getArchivedUsers(search));
    }

    @PutMapping("/users/{id}/restore")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "RESTORE_SYSTEM_USER", severity = Severity.INFO)
    public ResponseEntity<String> restoreUser(@PathVariable Long id) {
        adminArchiveService.restoreUser(id);
        return ResponseEntity.ok("System User restored successfully");
    }

    @DeleteMapping("/users/{id}")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "SOFT_DELETE_SYSTEM_USER", severity = Severity.INFO)
    public ResponseEntity<String> softDeleteUser(@PathVariable Long id) {
        adminArchiveService.softDeleteUser(id);
        return ResponseEntity.ok("System User moved to trash");
    }

    @DeleteMapping("/users/{id}/permanent")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "DELETE_SYSTEM_USER_PERMANENT", severity = Severity.WARNING)
    public ResponseEntity<String> deleteUserPermanent(@PathVariable Long id) {
        adminArchiveService.deleteUserPermanent(id);
        return ResponseEntity.ok("System User deleted permanently");
    }

    // --- Officers ---
    @GetMapping("/officers")
    public ResponseEntity<List<Map<String, Object>>> getArchivedOfficers(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(adminArchiveService.getArchivedOfficers(search));
    }

    @PutMapping("/officers/{id}/restore")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "RESTORE_OFFICER", severity = Severity.INFO)
    public ResponseEntity<String> restoreOfficer(@PathVariable Long id) {
        adminArchiveService.restoreOfficer(id);
        return ResponseEntity.ok("Officer restored successfully");
    }

    @DeleteMapping("/officers/{id}")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "SOFT_DELETE_OFFICER", severity = Severity.INFO)
    public ResponseEntity<String> softDeleteOfficer(@PathVariable Long id) {
        adminArchiveService.softDeleteOfficer(id);
        return ResponseEntity.ok("Officer moved to trash");
    }

    @DeleteMapping("/officers/{id}/permanent")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "DELETE_OFFICER_PERMANENT", severity = Severity.WARNING)
    public ResponseEntity<String> deleteOfficerPermanent(@PathVariable Long id) {
        adminArchiveService.deleteOfficerPermanent(id);
        return ResponseEntity.ok("Officer deleted permanently");
    }

    // --- Trash / Soft-Deleted Lists ---
    @GetMapping("/trash/residents")
    public ResponseEntity<List<Map<String, Object>>> getTrashResidents(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(adminArchiveService.getTrashResidents(search));
    }

    @GetMapping("/trash/users")
    public ResponseEntity<List<Map<String, Object>>> getTrashUsers(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(adminArchiveService.getTrashUsers(search));
    }

    @GetMapping("/trash/officers")
    public ResponseEntity<List<Map<String, Object>>> getTrashOfficers(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(adminArchiveService.getTrashOfficers(search));
    }
}
