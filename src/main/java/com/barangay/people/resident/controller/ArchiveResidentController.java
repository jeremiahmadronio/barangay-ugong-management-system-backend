package com.barangay.people.resident.controller;

import com.barangay.people.resident.service.ArchiveResidentService;
import com.barangay.zshared.audit.annotations.LogAudit;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/archive-resident")
@RequiredArgsConstructor
public class ArchiveResidentController {

    private final ArchiveResidentService archiveResidentService;


    @GetMapping("/residents")
    public ResponseEntity<List<Map<String, Object>>> getArchivedResidents(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(archiveResidentService.getArchivedResidents(search));
    }

    @PutMapping("/residents/{id}/restore")
    @LogAudit(module = AuditModule.RESIDENTS, action = "RESTORE_RESIDENT", severity = Severity.INFO)
    public ResponseEntity<String> restoreResident(@PathVariable Long id) {
        archiveResidentService.restoreResident(id);
        return ResponseEntity.ok("Resident restored successfully");
    }

    @DeleteMapping("/residents/{id}")
    @LogAudit(module = AuditModule.RESIDENTS, action = "SOFT_DELETE_RESIDENT", severity = Severity.INFO)
    public ResponseEntity<String> softDeleteResident(@PathVariable Long id) {
        archiveResidentService.softDeleteResident(id);
        return ResponseEntity.ok("Resident moved to trash");
    }

    @DeleteMapping("/residents/{id}/permanent")
    @LogAudit(module = AuditModule.RESIDENTS, action = "DELETE_RESIDENT_PERMANENT", severity = Severity.WARNING)
    public ResponseEntity<String> deleteResidentPermanent(@PathVariable Long id) {
        archiveResidentService.deleteResidentPermanent(id);
        return ResponseEntity.ok("Resident deleted permanently");
    }

    @GetMapping("/trash/residents")
    public ResponseEntity<List<Map<String, Object>>> getTrashResidents(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(archiveResidentService.getTrashResidents(search));
    }


}
