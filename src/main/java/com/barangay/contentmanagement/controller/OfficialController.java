package com.barangay.contentmanagement.controller;

import com.barangay.contentmanagement.dto.OfficialDto;
import com.barangay.contentmanagement.model.Official;
import com.barangay.contentmanagement.service.OfficialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OfficialController {

    private final OfficialService officialService;

    // Public endpoint for the landing page
    @GetMapping("/public/officials")
    public ResponseEntity<List<Official>> getPublicOfficials() {
        return ResponseEntity.ok(officialService.getPublicOfficials());
    }

    // Admin endpoints
    @GetMapping("/officials")
    public ResponseEntity<List<Official>> getAllOfficials() {
        return ResponseEntity.ok(officialService.getAllOfficials());
    }

    // Create official
    @PostMapping(value = "/officials", consumes = "multipart/form-data")
    public ResponseEntity<Official> createOfficial(@ModelAttribute OfficialDto officialDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(officialService.createOfficial(officialDto));
    }

    // Update official
    @PutMapping(value = "/officials/{id}", consumes = "multipart/form-data")
    public ResponseEntity<Official> updateOfficial(
            @PathVariable Long id,
            @ModelAttribute OfficialDto officialDto,
            @RequestParam(required = false, defaultValue = "false") boolean removeImage) {
        return ResponseEntity.ok(officialService.updateOfficial(id, officialDto, removeImage));
    }

    // Delete official
    @DeleteMapping("/officials/{id}")
    public ResponseEntity<Void> deleteOfficial(@PathVariable Long id) {
        officialService.deleteOfficial(id);
        return ResponseEntity.noContent().build();
    }

    // Archive official
    @PatchMapping("/officials/{id}/archive")
    public ResponseEntity<Void> archiveOfficial(@PathVariable Long id) {
        officialService.archiveOfficial(id);
        return ResponseEntity.ok().build();
    }
}
