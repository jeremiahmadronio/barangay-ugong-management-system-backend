package com.barangay.zshared.enumerated;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Tracks repeat occurrences for CAR status offenses.
 */
public enum CarOccurrenceCategory {
    FIRST_OCCURRENCE,
    SECOND_OCCURRENCE,
    THIRD_OR_SUBSEQUENT,
    UNKNOWN;

    @JsonCreator
    public static CarOccurrenceCategory fromString(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toUpperCase();
        if ("FIRST".equals(normalized) || "FIRST_OCCURRENCE".equals(normalized) || "1ST_TIME".equals(normalized)) {
            return FIRST_OCCURRENCE;
        }
        if ("SECOND".equals(normalized) || "SECOND_OCCURRENCE".equals(normalized) || "REPEATED_OCCURRENCE".equals(normalized) || "2ND_TIME".equals(normalized)) {
            return SECOND_OCCURRENCE;
        }
        if ("THIRD_OR_SUBSEQUENT".equals(normalized) || "HABITUAL_OCCURRENCE".equals(normalized) || "3RD_TIME".equals(normalized) || "CHRONIC".equals(normalized)) {
            return THIRD_OR_SUBSEQUENT;
        }
        try {
            return CarOccurrenceCategory.valueOf(normalized);
        } catch (Exception e) {
            return UNKNOWN;
        }
    }
}


