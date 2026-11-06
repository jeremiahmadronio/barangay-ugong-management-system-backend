package com.barangay.zshared.enumerated;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * High-level categorization of Child at Risk (CAR) acts and circumstances under Chapter V of RA 9344 / RA 10630.
 */
public enum CarActCategory {
    JUVENILE_STATUS_OFFENSE,
    LIGHT_OFFENSE_MISDEMEANOR,
    OFFENSE_NOT_APPLICABLE_TO_CHILDREN,
    CHILD_PROTECTION_VULNERABILITY;

    @JsonCreator
    public static CarActCategory fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return CarActCategory.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            return JUVENILE_STATUS_OFFENSE;
        }
    }
}


