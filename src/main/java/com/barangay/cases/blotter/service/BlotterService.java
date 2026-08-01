package com.barangay.cases.blotter.service;

import com.barangay.cases.sharedmodel.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.barangay.people.person.repository.PersonRepository;

/**
 * Business logic service for managing Blotter Cases (Formal Complaint and For the Record).
 */
@Service
@RequiredArgsConstructor
public class BlotterService {

    private final CaseRepository caseRepository;
    private final PersonRepository personRepository;






}
