package com.barangay.zshared.dto;
/**
 * Request payload for recording the incident details of a case.
 * Date and time are accepted as ISO-formatted strings
 * (e.g. {@code "2025-08-02"}, {@code "14:30"}) and parsed by
 * {@link com.barangay.zshared.mapper.IncidentDetailMapper}.
 */
public record IncidentCreateDto(
        String natureOfComplaint,
        String dateOfIncident,
        String timeOfIncident,
        String placeOfIncident,
        String frequencyOfIncident,
        String descriptionOfInjuries
) {}