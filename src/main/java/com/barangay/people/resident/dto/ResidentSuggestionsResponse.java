package com.barangay.people.resident.dto;

public record ResidentSuggestionsResponse(
        String suggestedBarangayId,
        String suggestedHouseholdNumber
) {}
