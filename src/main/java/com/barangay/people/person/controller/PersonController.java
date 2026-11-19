package com.barangay.people.person.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.barangay.people.person.dto.PersonOptionResponse;
import com.barangay.people.person.service.PersonService;

import lombok.RequiredArgsConstructor;

/**
 * REST controller providing lookup endpoints for Persons, resident autocomplete,
 * and user account provisioning candidates.
 */
@RestController
@RequestMapping("/api/v1/persons")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    /**
     * Searches available residents/persons who do not currently have a registered system user account.
     * Commonly used in the Admin user creation autocomplete dropdown.
     *
     * @param query the keyword or prefix to search against first and last names
     * @return a {@link ResponseEntity} containing a list of matching {@link PersonOptionResponse}
     */
    @GetMapping("/available-for-user")
    public ResponseEntity<List<PersonOptionResponse>> searchAvailablePersons(
            @RequestParam(name = "query", defaultValue = "") String query) {
        return ResponseEntity.ok(personService.searchAvailablePersonsForUserCreation(query));
    }
}
