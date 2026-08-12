package com.barangay.zshared.mapper;

import com.barangay.people.respondent.model.Respondent;
import com.barangay.people.person.model.Person;
import com.barangay.zshared.dto.RespondentCreateDto;
import com.barangay.zshared.dto.RespondentDetailDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Converts between {@link Respondent} entities and their corresponding DTOs.
 * Pure transformation only — persistence is handled by the calling service.
 */
@Component
@RequiredArgsConstructor
public class RespondentMapper {

    private final PersonMapper personMapper;

    public RespondentDetailDto toDetailDto(Respondent respondent) {
        if (respondent == null || respondent.getPerson() == null) return null;
        Person p = respondent.getPerson();

        return new RespondentDetailDto(
                respondent.getId(),
                p.getLastName(),
                p.getFirstName(),
                p.getMiddleName(),
                personMapper.buildFullName(p),
                p.getContactNumber(),
                p.getAge(),
                p.getGender(),
                p.getCivilStatus(),
                p.getOccupation(),
                personMapper.getCompleteAddress(p),
                respondent.getRelationshipToComplainant(),
                respondent.getLivingWithComplainant()
        );
    }

    /**
     * Builds a new, unsaved {@link Person} from the create request.
     *
     * @param dto the create request
     * @return a new, unsaved {@link Person} entity
     */
    public Person toNewPerson(RespondentCreateDto dto) {
        if (dto == null) return null;

        Person person = new Person();
        person.setLastName(dto.lastName());
        person.setFirstName(dto.firstName());
        person.setMiddleName(dto.middleName());
        person.setContactNumber(dto.contactNumber());
        person.setAge(dto.age());
        person.setGender(dto.gender());
        person.setCivilStatus(dto.civilStatus());
        person.setOccupation(dto.occupation());
        person.setCompleteAddress(dto.completeAddress());
        return person;
    }


    /**
     * Wraps an already-persisted {@link Person} in a new, unsaved {@link Respondent},
     * attaching relationship metadata from the original request.
     *
     * @param person the saved person to link
     * @param dto    the original create request, used for relationship fields
     * @return a new, unsaved {@link Respondent} entity
     */
    public Respondent toRespondent(Person person, RespondentCreateDto dto) {
        Respondent respondent = new Respondent();
        respondent.setPerson(person);
        respondent.setRelationshipToComplainant(dto.relationshipToComplainant());
        respondent.setLivingWithComplainant(dto.currentlyLivingWithComplainant());
        return respondent;
    }
}