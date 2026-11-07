package com.barangay.zshared.enumerated;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Recommended and designated management pathway for a Child in Conflict with the Law (CICL).
 */
public enum CiclPathway {
    /** Case intake pending initial LSWDO or BCPC assessment */
    PENDING,

    /** Assigned to community-based or institutional intervention (non-diversion, e.g. under 15 years old) */
    INTERVENTION,

    /** Undergoing formal Barangay or Police/LSWDO Diversion proceedings (15 to 18 years old with discernment) */
    DIVERSION;

    @JsonCreator
    public static CiclPathway fromString(String value) {
        if (value == null || value.isBlank()) {
            return PENDING;
        }
        String clean = value.trim().toUpperCase();
        if (clean.contains("DIVERSION")) {
            return DIVERSION;
        }
        if (clean.contains("INTERVENTION")) {
            return INTERVENTION;
        }
        for (CiclPathway cp : values()) {
            if (cp.name().equalsIgnoreCase(clean)) {
                return cp;
            }
        }
        return PENDING;
    }
}

