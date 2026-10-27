package com.barangay.zshared.dto;

/**
 * Read-model for the incident details of a case.
 */
public record IncidentDetailDto(
        Long id,
        String natureOfComplaint,
        String dateOfIncident,
        String timeOfIncident,
        String placeOfIncident,
        String frequencyOfIncident,
        String descriptionOfInjuries,
        String relatedBarangayService
) {}