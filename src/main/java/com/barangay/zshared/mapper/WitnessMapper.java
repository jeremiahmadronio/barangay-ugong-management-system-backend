package com.barangay.zshared.mapper;

import com.barangay.cases.sharedmodel.Case;
import com.barangay.people.person.model.Person;
import com.barangay.people.witness.model.Witness;
import com.barangay.zshared.dto.WitnessCreateDto;
import com.barangay.zshared.dto.WitnessDetailDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Converts between {@link Witness} entities and their corresponding DTOs.
 * Pure transformation only — persistence is handled by the calling service.
 */
@Component
@RequiredArgsConstructor
public class WitnessMapper {

    private final PersonMapper personMapper;

    public WitnessDetailDto toDetailDto(Witness witness) {
        if (witness == null) return null;
        Person p = witness.getPerson();

        return new WitnessDetailDto(
                witness.getId(),
                p != null ? p.getFirstName() : null,
                p != null ? p.getLastName() : null,
                personMapper.buildFullName(p),
                p != null ? p.getContactNumber() : null,
                personMapper.getCompleteAddress(p),
                witness.getTestimony(),
                witness.getTestimonyFile()
        );
    }

    /**
     * Builds a new, unsaved {@link Person} from the create request.
     * Missing name fields fall back to placeholder values ("Witness" / "N/A").
     * Returns {@code null} if both name fields are missing, signaling that
     * no witness should be created.
     *
     * @param dto the create request
     * @return a new, unsaved {@link Person}, or {@code null} if no name was provided
     */
    public Person toNewPerson(WitnessCreateDto dto) {
        if (dto == null) return null;
        if (dto.firstName() == null && dto.lastName() == null) return null;

        Person person = new Person();
        person.setFirstName(dto.firstName() != null ? dto.firstName() : "Witness");
        person.setLastName(dto.lastName() != null ? dto.lastName() : "N/A");
        person.setContactNumber(dto.contactNumber());
        person.setCompleteAddress(dto.completeAddress());
        return person;
    }

    /**
     * Wraps an already-persisted {@link Person} in a new, unsaved {@link Witness},
     * linking it to the given case.
     *
     * @param person the saved person to link
     * @param aCase  the case the witness belongs to
     * @param dto    the original create request, used for the testimony text
     * @return a new, unsaved {@link Witness} entity
     */
    public Witness toWitness(Person person, Case aCase, WitnessCreateDto dto) {
        Witness witness = new Witness();
        witness.setACase(aCase);
        witness.setPerson(person);
        witness.setTestimony(dto.testimonyText());
        return witness;
    }
}