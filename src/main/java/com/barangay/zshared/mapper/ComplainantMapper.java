package com.barangay.zshared.mapper;

import com.barangay.people.complainant.model.Complainant;
import com.barangay.people.person.model.Person;
import com.barangay.usermanagement.user.repository.UserRepository;
import com.barangay.zshared.dto.ComplainantCreateDto;
import com.barangay.zshared.dto.ComplainantDetailDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Converts between {@link Complainant} entities and their corresponding DTOs.
 * <p>
 * This mapper performs pure in-memory transformation only — it does not
 * persist any entities. Persistence of the underlying {@link Person} is the
 * responsibility of the calling service.
 */
@Component
@RequiredArgsConstructor
public class ComplainantMapper {

    private final PersonMapper personMapper;
    private final ObjectProvider<UserRepository> userRepositoryProvider;

    /**
     * Maps a {@link Complainant} entity to a {@link ComplainantDetailDto}.
     *
     * @param complainant the entity to convert, may be {@code null}
     * @return the corresponding {@link ComplainantDetailDto}, or {@code null} if input is {@code null}
     */
    public ComplainantDetailDto toDetailDto(Complainant complainant) {
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
        if ((email == null || email.isBlank()) && p.getId() != null) {
            UserRepository userRepo = userRepositoryProvider.getIfAvailable();
            if (userRepo != null) {
                email = userRepo.findFirstByPersonId(p.getId())
                        .map(u -> u.getSystemEmail())
                        .filter(e -> e != null && !e.isBlank())
                        .orElse(null);
            }
        }
        if ((email == null || email.isBlank()) && p.getResident() != null && p.getResident().getPerson() != null) {
            email = p.getResident().getPerson().getEmail();
        }

        return new ComplainantDetailDto(
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
                personMapper.getCompleteAddress(p)
        );
    }

    /**
     * Builds a new, unsaved {@link Person} from the create request.
     * The caller is responsible for persisting the {@link Person} and
     * assembling/saving the resulting {@link Complainant}.
     *
     * @param dto the create request
     * @return a new, unsaved {@link Person} entity
     */
    public Person toNewPerson(ComplainantCreateDto dto) {
        if (dto == null) {
            return null;
        }

        Person person = new Person();
        person.setLastName(dto.lastName());
        person.setFirstName(dto.firstName());
        person.setMiddleName(dto.middleName());
        person.setContactNumber(dto.contactNumber());
        person.setAge(dto.age());
        person.setGender(dto.gender());
        person.setCivilStatus(dto.civilStatus());
        person.setEmail(dto.email());
        person.setCompleteAddress(dto.completeAddress());
        return person;
    }

    /**
     * Wraps an already-persisted {@link Person} in a new, unsaved {@link Complainant}.
     *
     * @param person the saved person to link
     * @return a new, unsaved {@link Complainant} entity
     */
    public Complainant toComplainant(Person person) {
        Complainant complainant = new Complainant();
        complainant.setPerson(person);
        return complainant;
    }
}
