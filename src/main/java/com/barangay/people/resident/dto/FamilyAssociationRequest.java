package com.barangay.people.resident.dto;

public record FamilyAssociationRequest(
        Long relatedResidentId,
        String relationship,
        String reverseRelationship
) {}
