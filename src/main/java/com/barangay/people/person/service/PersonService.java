package com.barangay.people.person.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barangay.people.person.dto.PersonOptionResponse;
import com.barangay.people.person.model.Person;
import com.barangay.people.person.repository.PersonRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service managing Person lookups, autocomplete operations, and person-to-user selection logic.
 */
@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;

    /**
     * Searches available persons and residents who do not yet have an associated system user account.
     *
     * @param query the name search keyword
     * @return a list of {@link PersonOptionResponse} mapped for dropdowns
     */
    @Transactional(readOnly = true)
    public List<PersonOptionResponse> searchAvailablePersonsForUserCreation(String query) {
        return personRepository.findPersonsWithoutUserAccount(query)
                .stream()
                .map(this::mapToOptionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Maps a {@link Person} entity to a lightweight {@link PersonOptionResponse} DTO.
     *
     * @param person the person entity to map
     * @return a mapped {@link PersonOptionResponse}
     */
    private PersonOptionResponse mapToOptionResponse(Person person) {
        String fullName = (person.getFirstName() + " " +
                (person.getMiddleName() != null && !person.getMiddleName().isBlank() ? person.getMiddleName() + " " : "") +
                person.getLastName() +
                (person.getSuffix() != null && !person.getSuffix().isBlank() ? " " + person.getSuffix() : "")).trim();

        return new PersonOptionResponse(
                person.getId(),
                person.getFirstName(),
                person.getLastName(),
                fullName,
                person.getContactNumber(),
                person.getCompleteAddress()
        );
    }
}
