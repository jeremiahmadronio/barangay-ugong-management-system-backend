package com.barangay.people.person.controller;

import com.barangay.people.person.dto.PersonOptionResponse;
import com.barangay.people.person.service.PersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller exposing endpoints for Person search and resident lookups.
 */
@RestController
@RequestMapping("/api/v1/persons")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    /**
     * Endpoint to search available residents/persons who do not have a user account yet.
     * Used in the Create User modal autocomplete search input.
     */
    @GetMapping("/available-for-user")
    public ResponseEntity<List<PersonOptionResponse>> searchAvailablePersons(@RequestParam(name = "query", defaultValue = "") String query) {
        return ResponseEntity.ok(personService.searchAvailablePersonsForUserCreation(query));
    }
}
