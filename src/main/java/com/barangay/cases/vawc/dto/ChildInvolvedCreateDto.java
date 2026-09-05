package com.barangay.cases.vawc.dto;

/**
 * Request payload for adding a child involved / affected in a VAWC case.
 */
public record ChildInvolvedCreateDto(
        String firstName,
        String middleName,
        String lastName,
        String suffix,
        Integer age,
        String role // e.g. "Victim", "Witness", "Dependent"
) {
    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) sb.append(firstName.trim());
        if (middleName != null && !middleName.isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(middleName.trim());
        }
        if (lastName != null && !lastName.isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(lastName.trim());
        }
        if (suffix != null && !suffix.isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(suffix.trim());
        }
        return sb.toString();
    }
}
