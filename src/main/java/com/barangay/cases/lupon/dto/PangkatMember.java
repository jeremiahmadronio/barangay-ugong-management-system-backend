package com.barangay.cases.lupon.dto;

/**
 * Request payload representing an individual officer assigned to a Pangkat Tagapagkasundo panel.
 *
 * @param officerId unique database identifier of the assigned {@link com.barangay.people.officer.model.Officer}
 * @param position  designated role in the panel (e.g., "CHAIRMAN", "SECRETARY", "MEMBER")
 */
public record PangkatMember (
        Long officerId,
        String position
) {
}
