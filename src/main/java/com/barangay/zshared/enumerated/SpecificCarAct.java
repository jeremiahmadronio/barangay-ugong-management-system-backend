package com.barangay.zshared.enumerated;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Granular acts or conditions falling under Child at Risk (CAR) classification.
 */
public enum SpecificCarAct {
    // Juvenile Status Offenses
    CURFEW_VIOLATION(CarActCategory.JUVENILE_STATUS_OFFENSE),
    TRUANCY(CarActCategory.JUVENILE_STATUS_OFFENSE),
    PARENTAL_DISOBEDIENCE(CarActCategory.JUVENILE_STATUS_OFFENSE),
    ANTI_SMOKING_VIOLATION(CarActCategory.JUVENILE_STATUS_OFFENSE),
    ANTI_DRINKING_VIOLATION(CarActCategory.JUVENILE_STATUS_OFFENSE),

    // Light Offenses / Misdemeanors
    DISORDERLY_CONDUCT(CarActCategory.LIGHT_OFFENSE_MISDEMEANOR),
    PUBLIC_SCANDAL(CarActCategory.LIGHT_OFFENSE_MISDEMEANOR),
    HARASSMENT(CarActCategory.LIGHT_OFFENSE_MISDEMEANOR),
    DRUNKENNESS(CarActCategory.LIGHT_OFFENSE_MISDEMEANOR),
    PUBLIC_INTOXICATION(CarActCategory.LIGHT_OFFENSE_MISDEMEANOR),
    CRIMINAL_NUISANCE(CarActCategory.LIGHT_OFFENSE_MISDEMEANOR),
    VANDALISM(CarActCategory.LIGHT_OFFENSE_MISDEMEANOR),
    GAMBLING(CarActCategory.LIGHT_OFFENSE_MISDEMEANOR),
    LITTERING(CarActCategory.LIGHT_OFFENSE_MISDEMEANOR),
    PUBLIC_URINATION(CarActCategory.LIGHT_OFFENSE_MISDEMEANOR),
    TRESPASSING(CarActCategory.LIGHT_OFFENSE_MISDEMEANOR),
    PETTY_THEFT(CarActCategory.LIGHT_OFFENSE_MISDEMEANOR),
    PUBLIC_DISTURBANCE(CarActCategory.LIGHT_OFFENSE_MISDEMEANOR),
    PHYSICAL_INJURIES_SLIGHT(CarActCategory.LIGHT_OFFENSE_MISDEMEANOR),

    // Offenses Not Applicable To Children (Decriminalized for Minors by RA 9344)
    PROSTITUTION_EXEMPT_ACT(CarActCategory.OFFENSE_NOT_APPLICABLE_TO_CHILDREN),
    MENDICANCY_EXEMPT(CarActCategory.OFFENSE_NOT_APPLICABLE_TO_CHILDREN),
    SNIFFING_RUGBY_EXEMPT(CarActCategory.OFFENSE_NOT_APPLICABLE_TO_CHILDREN),
    VAGRANCY(CarActCategory.OFFENSE_NOT_APPLICABLE_TO_CHILDREN),

    // Child Protection / Vulnerability
    CHILD_ABUSE_VICTIM(CarActCategory.CHILD_PROTECTION_VULNERABILITY),
    CHILD_EXPLOITATION_VICTIM(CarActCategory.CHILD_PROTECTION_VULNERABILITY),
    ABANDONED_OR_NEGLECTED(CarActCategory.CHILD_PROTECTION_VULNERABILITY),
    STREET_CHILD(CarActCategory.CHILD_PROTECTION_VULNERABILITY),
    RUNAWAY(CarActCategory.CHILD_PROTECTION_VULNERABILITY),

    // Fallbacks
    OTHER_JUVENILE_STATUS_OFFENSE(CarActCategory.JUVENILE_STATUS_OFFENSE),
    OTHER_LIGHT_OFFENSE(CarActCategory.LIGHT_OFFENSE_MISDEMEANOR),
    OTHER_NOT_APPLICABLE(CarActCategory.OFFENSE_NOT_APPLICABLE_TO_CHILDREN),
    OTHER_CHILD_PROTECTION(CarActCategory.CHILD_PROTECTION_VULNERABILITY);

    private final CarActCategory category;

    SpecificCarAct(CarActCategory category) {
        this.category = category;
    }

    public CarActCategory getCategory() {
        return category;
    }

    @JsonCreator
    public static SpecificCarAct fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String clean = value.trim().toUpperCase();
        try {
            return SpecificCarAct.valueOf(clean);
        } catch (IllegalArgumentException ignored) {
            switch (clean) {
                case "MINOR_MUTILATION_OR_GRAFFITI":
                case "GRAFFITI":
                    return VANDALISM;
                case "SMOKING_OR_VAPING":
                case "SMOKING":
                case "VAPING":
                    return ANTI_SMOKING_VIOLATION;
                case "ALCOHOL_CONSUMPTION":
                case "DRINKING":
                    return ANTI_DRINKING_VIOLATION;
                case "SUBSTANCE_GLUE_SNIFFING":
                case "GLUE_SNIFFING":
                    return SNIFFING_RUGBY_EXEMPT;
                case "VAGRANCY_MENDICANCY":
                    return MENDICANCY_EXEMPT;
                case "DISORDERLY_CONDUCT_SCANDAL":
                    return DISORDERLY_CONDUCT;
                case "ALARM_AND_SCANDAL":
                    return PUBLIC_SCANDAL;
                case "LOCAL_ORDINANCE_VIOLATION":
                case "UNJUST_VEXATION_LIGHT":
                case "OTHER_STATUS_OFFENSE":
                    return OTHER_LIGHT_OFFENSE;
                default:
                    return OTHER_LIGHT_OFFENSE;
            }
        }
    }
}
