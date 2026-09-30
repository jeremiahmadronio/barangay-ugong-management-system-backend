package com.barangay.contentmanagement.controller;

import com.barangay.contentmanagement.dto.AnnouncementDto;
import com.barangay.contentmanagement.model.Announcement;
import com.barangay.contentmanagement.service.AnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cms/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    // Public endpoint for the landing page
    @GetMapping("/public")
    public ResponseEntity<List<Announcement>> getPublicAnnouncements() {
        return ResponseEntity.ok(announcementService.getPublicAnnouncements());
    }

    // Admin endpoints
    @GetMapping
    public ResponseEntity<List<Announcement>> getAllAnnouncements() {
        return ResponseEntity.ok(announcementService.getAllAnnouncements());
    }

    @PostMapping
    public ResponseEntity<Announcement> createAnnouncement(@ModelAttribute @Valid AnnouncementDto dto) {
        return ResponseEntity.ok(announcementService.createAnnouncement(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Announcement> updateAnnouncement(
            @PathVariable Long id,
            @ModelAttribute @Valid AnnouncementDto dto) {
        return ResponseEntity.ok(announcementService.updateAnnouncement(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAnnouncement(@PathVariable Long id) {
        announcementService.deleteAnnouncement(id);
        return ResponseEntity.ok("Announcement deleted successfully");
    }

    @PatchMapping("/{id}/toggle-publish")
    public ResponseEntity<Announcement> togglePublish(@PathVariable Long id) {
        return ResponseEntity.ok(announcementService.togglePublish(id));
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<Announcement> archiveAnnouncement(@PathVariable Long id) {
        return ResponseEntity.ok(announcementService.archiveAnnouncement(id));
    }
}
