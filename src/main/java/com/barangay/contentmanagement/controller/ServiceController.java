package com.barangay.contentmanagement.controller;

import com.barangay.contentmanagement.dto.ServiceDto;
import com.barangay.contentmanagement.model.BarangayService;
import com.barangay.contentmanagement.service.BarangayServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cms/services")
@RequiredArgsConstructor
public class ServiceController {

    private final BarangayServiceService barangayServiceService;

    @GetMapping("/public")
    public ResponseEntity<List<BarangayService>> getPublicServices() {
        return ResponseEntity.ok(barangayServiceService.getPublicServices());
    }

    @GetMapping
    public ResponseEntity<List<BarangayService>> getAllServices() {
        return ResponseEntity.ok(barangayServiceService.getAllServices());
    }

    @PostMapping
    public ResponseEntity<BarangayService> createService(@RequestBody @Valid ServiceDto dto) {
        return ResponseEntity.ok(barangayServiceService.createService(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BarangayService> updateService(
            @PathVariable Long id,
            @RequestBody @Valid ServiceDto dto) {
        return ResponseEntity.ok(barangayServiceService.updateService(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteService(@PathVariable Long id) {
        barangayServiceService.deleteService(id);
        return ResponseEntity.ok("Service deleted successfully");
    }

    @PatchMapping("/{id}/toggle-publish")
    public ResponseEntity<BarangayService> togglePublish(@PathVariable Long id) {
        return ResponseEntity.ok(barangayServiceService.togglePublish(id));
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<BarangayService> archiveService(@PathVariable Long id) {
        return ResponseEntity.ok(barangayServiceService.archiveService(id));
    }
}
