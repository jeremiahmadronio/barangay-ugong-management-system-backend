package com.barangay.contentmanagement.controller;

import com.barangay.contentmanagement.dto.AnnouncementDto;
import com.barangay.contentmanagement.model.Announcement;
import com.barangay.contentmanagement.service.AnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing public and administrative portal announcements.
 */
@RestController
@RequestMapping("/api/v1/cms/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    /**
     * Retrieves active published announcements for public viewing.
     *
     * @return list of published announcements
     */
    @GetMapping("/public")
    public ResponseEntity<List<Announcement>> getPublicAnnouncements() {
        return ResponseEntity.ok(announcementService.getPublicAnnouncements());
    }

    /**
     * Retrieves all active announcements for administrative management.
     *
     * @return list of active announcements
     */
    @GetMapping
    public ResponseEntity<List<Announcement>> getAllAnnouncements() {
        return ResponseEntity.ok(announcementService.getAllAnnouncements());
    }

    /**
     * Creates a new announcement with optional image attachments.
     *
     * @param dto announcement payload
     * @return created announcement
     */
    @PostMapping
    public ResponseEntity<Announcement> createAnnouncement(@ModelAttribute @Valid AnnouncementDto dto) {
        return ResponseEntity.ok(announcementService.createAnnouncement(dto));
    }

    /**
     * Updates an existing announcement record.
     *
     * @param id  announcement identifier
     * @param dto updated announcement payload
     * @return updated announcement
     */
    @PutMapping("/{id}")
    public ResponseEntity<Announcement> updateAnnouncement(
            @PathVariable Long id,
            @ModelAttribute @Valid AnnouncementDto dto
    ) {
        return ResponseEntity.ok(announcementService.updateAnnouncement(id, dto));
    }

    /**
     * Permanently deletes an announcement.
     *
     * @param id announcement identifier
     * @return confirmation message
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAnnouncement(@PathVariable Long id) {
        announcementService.deleteAnnouncement(id);
        return ResponseEntity.ok("Announcement deleted successfully");
    }

    /**
     * Toggles the publication visibility status of an announcement.
     *
     * @param id announcement identifier
     * @return updated announcement
     */
    @PatchMapping("/{id}/toggle-publish")
    public ResponseEntity<Announcement> togglePublish(@PathVariable Long id) {
        return ResponseEntity.ok(announcementService.togglePublish(id));
    }

    /**
     * Archives an announcement, removing it from active lists.
     *
     * @param id announcement identifier
     * @return archived announcement
     */
    @PatchMapping("/{id}/archive")
    public ResponseEntity<Announcement> archiveAnnouncement(@PathVariable Long id) {
        return ResponseEntity.ok(announcementService.archiveAnnouncement(id));
    }
}
