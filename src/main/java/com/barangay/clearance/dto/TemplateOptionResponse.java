package com.barangay.clearance.dto;

/**
 * Lightweight DTO for rendering clearance document type selection cards and dropdowns.
 * Only transfers the essential identification and display title.
 */
public record TemplateOptionResponse(
        Long id,
        String certTitle
) {}
