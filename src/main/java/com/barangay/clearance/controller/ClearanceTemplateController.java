package com.barangay.clearance.controller;

import com.barangay.clearance.dto.TemplateDetailResponse;
import com.barangay.clearance.dto.TemplateOptionResponse;
import com.barangay.clearance.service.ClearanceTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller exposing endpoints for certificate and clearance templates.
 */
@RestController
@RequestMapping("/api/v1/clearance/template")
@RequiredArgsConstructor
public class ClearanceTemplateController {

    private final ClearanceTemplateService clearanceTemplateService;

    /**
     * Endpoint to fetch lightweight active template options (id and certTitle).
     * Populates the Document Type selection grid in the issuance form.
     *
     * @return 200 OK with list of {@link TemplateOptionResponse}
     */
    @GetMapping({"", "/options", "/view-template"})
    public ResponseEntity<List<TemplateOptionResponse>> getTemplateOptions() {
        return ResponseEntity.ok(clearanceTemplateService.getTemplateOptions());
    }

    /**
     * Endpoint to fetch full template details by ID for rendering the issuance form.
     *
     * @param id Template ID
     * @return 200 OK with {@link TemplateDetailResponse}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TemplateDetailResponse> getTemplateById(@PathVariable Long id) {
        return ResponseEntity.ok(clearanceTemplateService.getTemplateById(id));
    }
}
