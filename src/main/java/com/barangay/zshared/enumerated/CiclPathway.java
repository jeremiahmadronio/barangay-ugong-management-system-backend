package com.barangay.zshared.enumerated;

/**
 * Recommended and designated management pathway for a Child in Conflict with the Law (CICL).
 */
public enum CiclPathway {
    /** Case intake pending initial LSWDO or BCPC assessment */
    PENDING,

    /** Assigned to community-based or institutional intervention (non-diversion, e.g. under 15 years old) */
    INTERVENTION,

    /** Undergoing formal Barangay or Police/LSWDO Diversion proceedings (15 to 18 years old with discernment) */
    DIVERSION
}

