package com.barangay.contentmanagement.controller;

import com.barangay.contentmanagement.dto.ServiceDto;
import com.barangay.contentmanagement.model.BarangayService;
import com.barangay.contentmanagement.service.BarangayServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing barangay public service catalog and guidelines.
 */
@RestController
@RequestMapping("/api/v1/cms/services")
@RequiredArgsConstructor
public class ServiceController {

    private final BarangayServiceService barangayServiceService;

    /**
     * Retrieves active published services for public viewing.
     *
     * @return list of published services
     */
    @GetMapping("/public")
    public ResponseEntity<List<BarangayService>> getPublicServices() {
        return ResponseEntity.ok(barangayServiceService.getPublicServices());
    }

    /**
     * Retrieves all active services for administrative management.
     *
     * @return list of all active services
     */
    @GetMapping
    public ResponseEntity<List<BarangayService>> getAllServices() {
        return ResponseEntity.ok(barangayServiceService.getAllServices());
    }

    /**
     * Creates a new barangay service entry.
     *
     * @param dto service details
     * @return created service
     */
    @PostMapping
    public ResponseEntity<BarangayService> createService(@RequestBody @Valid ServiceDto dto) {
        return ResponseEntity.ok(barangayServiceService.createService(dto));
    }

    /**
     * Updates an existing service entry.
     *
     * @param id  service identifier
     * @param dto updated service payload
     * @return updated service
     */
    @PutMapping("/{id}")
    public ResponseEntity<BarangayService> updateService(
            @PathVariable Long id,
            @RequestBody @Valid ServiceDto dto
    ) {
        return ResponseEntity.ok(barangayServiceService.updateService(id, dto));
    }

    /**
     * Permanently deletes a service entry.
     *
     * @param id service identifier
     * @return confirmation message
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteService(@PathVariable Long id) {
        barangayServiceService.deleteService(id);
        return ResponseEntity.ok("Service deleted successfully");
    }

    /**
     * Toggles publication visibility of a service entry.
     *
     * @param id service identifier
     * @return updated service
     */
    @PatchMapping("/{id}/toggle-publish")
    public ResponseEntity<BarangayService> togglePublish(@PathVariable Long id) {
        return ResponseEntity.ok(barangayServiceService.togglePublish(id));
    }

    /**
     * Archives a service entry.
     *
     * @param id service identifier
     * @return archived service
     */
    @PatchMapping("/{id}/archive")
    public ResponseEntity<BarangayService> archiveService(@PathVariable Long id) {
        return ResponseEntity.ok(barangayServiceService.archiveService(id));
    }
}
