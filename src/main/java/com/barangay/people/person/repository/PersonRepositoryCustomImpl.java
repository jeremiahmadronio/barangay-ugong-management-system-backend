package com.barangay.people.person.repository;

import com.barangay.people.person.model.Person;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;

/**
 * Implementation of custom manual queries for Person.
 * Uses EntityManager to execute optimized JPQL queries filtering out persons with existing user accounts.
 */
public class PersonRepositoryCustomImpl implements PersonRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Person> findPersonsWithoutUserAccount(String queryStr) {
        String jpql = """
                SELECT p FROM Person p
                WHERE (
                    LOWER(p.firstName) LIKE LOWER(:searchTerm) OR
                    LOWER(p.lastName) LIKE LOWER(:searchTerm) OR
                    LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(:searchTerm)
                )
                AND p.id NOT IN (
                    SELECT u.person.id FROM User u WHERE u.person.id IS NOT NULL
                )
                ORDER BY p.lastName ASC, p.firstName ASC
                """;

        TypedQuery<Person> query = entityManager.createQuery(jpql, Person.class);
        query.setParameter("searchTerm", "%" + (queryStr != null ? queryStr.trim() : "") + "%");
        query.setMaxResults(20);

        return query.getResultList();
    }
}
