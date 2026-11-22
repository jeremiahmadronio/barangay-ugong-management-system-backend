package com.barangay.contentmanagement.controller;

import com.barangay.contentmanagement.dto.OfficialDto;
import com.barangay.contentmanagement.model.Official;
import com.barangay.contentmanagement.service.OfficialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing barangay and SK official directory listings.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OfficialController {

    private final OfficialService officialService;

    /**
     * Retrieves active official directory entries for public viewing.
     *
     * @return list of active officials
     */
    @GetMapping("/public/officials")
    public ResponseEntity<List<Official>> getPublicOfficials() {
        return ResponseEntity.ok(officialService.getPublicOfficials());
    }

    /**
     * Retrieves all active officials for administration.
     *
     * @return list of all active officials
     */
    @GetMapping("/officials")
    public ResponseEntity<List<Official>> getAllOfficials() {
        return ResponseEntity.ok(officialService.getAllOfficials());
    }

    /**
     * Registers a new official profile with photo attachment.
     *
     * @param officialDto official details and image
     * @return created official
     */
    @PostMapping(value = "/officials", consumes = "multipart/form-data")
    public ResponseEntity<Official> createOfficial(@ModelAttribute OfficialDto officialDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(officialService.createOfficial(officialDto));
    }

    /**
     * Updates an existing official profile.
     *
     * @param id          official identifier
     * @param officialDto updated details
     * @param removeImage flag indicating photo removal
     * @return updated official
     */
    @PutMapping(value = "/officials/{id}", consumes = "multipart/form-data")
    public ResponseEntity<Official> updateOfficial(
            @PathVariable Long id,
            @ModelAttribute OfficialDto officialDto,
            @RequestParam(required = false, defaultValue = "false") boolean removeImage
    ) {
        return ResponseEntity.ok(officialService.updateOfficial(id, officialDto, removeImage));
    }

    /**
     * Permanently deletes an official record.
     *
     * @param id official identifier
     * @return empty response
     */
    @DeleteMapping("/officials/{id}")
    public ResponseEntity<Void> deleteOfficial(@PathVariable Long id) {
        officialService.deleteOfficial(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Archives an official profile.
     *
     * @param id official identifier
     * @return empty response
     */
    @PatchMapping("/officials/{id}/archive")
    public ResponseEntity<Void> archiveOfficial(@PathVariable Long id) {
        officialService.archiveOfficial(id);
        return ResponseEntity.ok().build();
    }
}
