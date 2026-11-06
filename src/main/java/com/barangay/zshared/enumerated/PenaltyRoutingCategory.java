package com.barangay.zshared.enumerated;

/**
 * Penalty tier determining whether diversion can be conducted at Barangay level (RA 9344 Sec. 23).
 * Offenses with imposable penalty of not more than 6 years imprisonment fall under Barangay Diversion.
 */
public enum PenaltyRoutingCategory {
    SIX_YEARS_OR_LESS,
    ABOVE_SIX_YEARS,
    UNDETERMINED
}

