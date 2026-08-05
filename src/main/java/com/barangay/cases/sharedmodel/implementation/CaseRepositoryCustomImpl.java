package com.barangay.cases.sharedmodel.implementation;

import com.barangay.cases.sharedmodel.custom.CaseRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

public class CaseRepositoryCustomImpl implements CaseRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;


}
