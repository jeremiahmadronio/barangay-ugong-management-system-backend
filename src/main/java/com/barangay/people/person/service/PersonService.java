package com.barangay.people.person.service;

import com.barangay.people.person.dto.PersonOptionResponse;
import com.barangay.people.person.model.Person;
import com.barangay.people.person.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service handling Person queries for resident selection and account creation.
 */
@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;

    /**
     * Searches available residents/persons who do not have a user account yet.
     */
    @Transactional(readOnly = true)
    public List<PersonOptionResponse> searchAvailablePersonsForUserCreation(String query) {
        return personRepository.findPersonsWithoutUserAccount(query)
                .stream()
                .map(this::mapToOptionResponse)
                .collect(Collectors.toList());
    }

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
