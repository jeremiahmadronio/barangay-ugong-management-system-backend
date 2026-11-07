package com.barangay.zshared.enumerated;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Penalty tier determining whether diversion can be conducted at Barangay level (RA 9344 Sec. 23).
 * Offenses with imposable penalty of not more than 6 years imprisonment fall under Barangay Diversion.
 */
public enum PenaltyRoutingCategory {
    SIX_YEARS_OR_LESS,
    ABOVE_SIX_YEARS,
    UNDETERMINED;

    @JsonCreator
    public static PenaltyRoutingCategory fromString(String value) {
        if (value == null || value.isBlank()) {
            return UNDETERMINED;
        }
        String clean = value.trim().toUpperCase();
        if (clean.contains("ABOVE") || clean.contains("EXCEEDING") || clean.contains("MORE_THAN_SIX")) {
            return ABOVE_SIX_YEARS;
        }
        if (clean.contains("SIX") || clean.contains("LESS") || clean.contains("LIGHT_OFFENSE") || clean.contains("BARANGAY")) {
            return SIX_YEARS_OR_LESS;
        }
        for (PenaltyRoutingCategory pr : values()) {
            if (pr.name().equalsIgnoreCase(clean)) {
                return pr;
            }
        }
        return UNDETERMINED;
    }
}

