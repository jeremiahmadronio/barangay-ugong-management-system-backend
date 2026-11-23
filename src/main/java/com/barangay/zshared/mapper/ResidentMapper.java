package com.barangay.zshared.mapper;

import com.barangay.people.complainant.model.Complainant;
import com.barangay.people.person.model.Person;
import com.barangay.people.resident.model.Resident;
import com.barangay.zshared.dto.ResidentDetailDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Converts between resident or complainant entities and {@link ResidentDetailDto}.
 */
@Component
@RequiredArgsConstructor
public class ResidentMapper {

    private final PersonMapper personMapper;

    /**
     * Maps a {@link Complainant} entity to a {@link ResidentDetailDto}.
     *
     * @param complainant the complainant entity, may be {@code null}
     * @return the mapped resident detail DTO, or {@code null} if input is {@code null}
     */
    public ResidentDetailDto toDetailDto(Complainant complainant) {
        if (complainant == null || complainant.getPerson() == null) {
            return null;
        }
        Person p = complainant.getPerson();

        Short age = p.getAge();
        if ((age == null || age == 0) && p.getBirthDate() != null) {
            age = (short) p.getCalculatedAge();
        }
        String email = p.getEmail();
        if (email == null || email.isBlank()) {
            email = p.getDisplayEmail();
        }

        return new ResidentDetailDto(
                complainant.getId(),
                p.getLastName(),
                p.getFirstName(),
                p.getMiddleName(),
                personMapper.buildFullName(p),
                p.getContactNumber(),
                age,
                p.getGender(),
                p.getCivilStatus(),
                email,
                personMapper.getCompleteAddress(p),
                p.getResident() != null ? p.getResident().getDateOfResidency() : null,
                p.getResident() != null ? p.getResident().getBarangayIdNumber() : null
        );
    }

    /**
     * Maps a {@link Resident} entity to a {@link ResidentDetailDto}.
     *
     * @param resident the resident entity, may be {@code null}
     * @return the mapped resident detail DTO, or {@code null} if input is {@code null}
     */
    public ResidentDetailDto toDetailDto(Resident resident) {
        if (resident == null || resident.getPerson() == null) {
            return null;
        }
        Person p = resident.getPerson();

        Short age = p.getAge();
        if ((age == null || age == 0) && p.getBirthDate() != null) {
            age = (short) p.getCalculatedAge();
        }
        String email = p.getEmail();
        if (email == null || email.isBlank()) {
            email = p.getDisplayEmail();
        }

        return new ResidentDetailDto(
                resident.getResidentId(),
                p.getLastName(),
                p.getFirstName(),
                p.getMiddleName(),
                personMapper.buildFullName(p),
                p.getContactNumber(),
                age,
                p.getGender(),
                p.getCivilStatus(),
                email,
                personMapper.getCompleteAddress(p),
                resident.getDateOfResidency(),
                resident.getBarangayIdNumber()
        );
    }
}
