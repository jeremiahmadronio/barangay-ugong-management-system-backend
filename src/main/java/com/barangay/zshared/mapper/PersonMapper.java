package com.barangay.zshared.mapper;

import com.barangay.people.person.model.Person;
import org.springframework.stereotype.Component;
/**
 * Shared helper for building display-friendly values from a {@link Person}
 * entity, used across the complainant, respondent, and witness mappers.
 * Contains no persistence logic — pure transformation only.
 */
@Component
public class PersonMapper {
    /**
     * Builds a "First Middle Last" display name, skipping any blank middle name.
     *
     * @param p the person, may be {@code null}
     * @return the formatted full name, or {@code null} if {@code p} is {@code null}
     */
    public String buildFullName(Person p) {
        if (p == null) return null;
        StringBuilder sb = new StringBuilder();
        if (p.getFirstName() != null) sb.append(p.getFirstName()).append(" ");
        if (p.getMiddleName() != null && !p.getMiddleName().isBlank()) sb.append(p.getMiddleName()).append(" ");
        if (p.getLastName() != null) sb.append(p.getLastName());
        return sb.toString().trim();
    }

    /**
     * Returns the person's complete address.
     *
     * @param p the person, may be {@code null}
     * @return the address, or {@code null} if {@code p} is {@code null}
     */
    public String getCompleteAddress(Person p) {
        return p != null ? p.getCompleteAddress() : null;
    }
}