package com.barangay.zshared.enumerated;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Entity or committee responsible for conducting diversion proceedings under RA 9344.
 */
public enum DiversionAuthority {
    BARANGAY,
    LSWDO,
    WCPD,
    COURT,
    UNDETERMINED;

    @JsonCreator
    public static DiversionAuthority fromString(String value) {
        if (value == null || value.isBlank()) {
            return UNDETERMINED;
        }
        String clean = value.trim().toUpperCase();
        if (clean.contains("PNP") || clean.contains("WCPD") || clean.contains("LAW_ENFORCEMENT") || clean.contains("POLICE")) {
            return WCPD;
        }
        if (clean.contains("BARANGAY") || clean.contains("BCPC")) {
            return BARANGAY;
        }
        if (clean.contains("LSWDO") || clean.contains("CSWDO") || clean.contains("SOCIAL_WORK")) {
            return LSWDO;
        }
        if (clean.contains("COURT") || clean.contains("BAHAY_PAG_ASA") || clean.contains("JUDICIAL")) {
            return COURT;
        }
        for (DiversionAuthority da : values()) {
            if (da.name().equalsIgnoreCase(clean)) {
                return da;
            }
        }
        return UNDETERMINED;
    }
}

