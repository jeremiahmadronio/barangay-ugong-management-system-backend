package com.barangay.zshared.enumerated;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Determination of the child's capacity to understand the consequences of their action at the time of the offense.
 */
public enum DiscernmentStatus {
    WITH_DISCERNMENT,
    WITHOUT_DISCERNMENT,
    PENDING_LSWDO_EVALUATION;

    @JsonCreator
    public static DiscernmentStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return PENDING_LSWDO_EVALUATION;
        }
        String clean = value.trim().toUpperCase();
        if (clean.contains("WITHOUT") || clean.contains("EXEMPT") || clean.contains("UNDER_15") || clean.contains("NOT_APPLICABLE")) {
            return WITHOUT_DISCERNMENT;
        }
        if (clean.contains("WITH_DISCERNMENT") || clean.equals("WITH")) {
            return WITH_DISCERNMENT;
        }
        if (clean.contains("PENDING") || clean.contains("EVALUATION") || clean.contains("ASSESSMENT") || clean.contains("LSWDO")) {
            return PENDING_LSWDO_EVALUATION;
        }
        for (DiscernmentStatus ds : values()) {
            if (ds.name().equalsIgnoreCase(clean)) {
                return ds;
            }
        }
        return PENDING_LSWDO_EVALUATION;
    }
}

