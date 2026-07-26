package com.barangay.people.person.repository;

import com.barangay.people.person.model.Person;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;

/**
 * Implementation of custom manual queries for Person.
 * Executes optimized JPQL queries filtering out persons with existing user accounts.
 * Prefix-based search matching starting letters (e.g. "JE" -> Jeremia, Jericho, Jerez) first.
 */
public class PersonRepositoryCustomImpl implements PersonRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Person> findPersonsWithoutUserAccount(String queryStr) {
        String cleanQuery = (queryStr != null) ? queryStr.trim() : "";

        if (cleanQuery.isEmpty()) {
            String jpql = """
                    SELECT p FROM Person p
                    WHERE p.id NOT IN (
                        SELECT u.person.id FROM User u WHERE u.person.id IS NOT NULL
                    )
                    ORDER BY p.lastName ASC, p.firstName ASC
                    """;

            TypedQuery<Person> query = entityManager.createQuery(jpql, Person.class);
            query.setMaxResults(20);
            return query.getResultList();
        }

        // Prefix match starting with the typed letters (e.g. "JE%")
        String prefixSearch = cleanQuery + "%";

        String jpql = """
                SELECT p FROM Person p
                WHERE (
                    LOWER(p.firstName) LIKE LOWER(:prefixSearch) OR
                    LOWER(p.lastName) LIKE LOWER(:prefixSearch) OR
                    LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(:prefixSearch)
                )
                AND p.id NOT IN (
                    SELECT u.person.id FROM User u WHERE u.person.id IS NOT NULL
                )
                ORDER BY
                    CASE
                        WHEN LOWER(p.firstName) LIKE LOWER(:prefixSearch) THEN 1
                        WHEN LOWER(p.lastName) LIKE LOWER(:prefixSearch) THEN 2
                        ELSE 3
                    END,
                    p.firstName ASC, p.lastName ASC
                """;

        TypedQuery<Person> query = entityManager.createQuery(jpql, Person.class);
        query.setParameter("prefixSearch", prefixSearch);
        query.setMaxResults(20);

        return query.getResultList();
    }
}
