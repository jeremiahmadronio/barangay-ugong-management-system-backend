package com.barangay.people.person.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.barangay.people.person.model.Person;

/**
 * Spring Data JPA repository for managing Person records, autocomplete lookups,
 * and user provisioning availability checks.
 */
@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {

    // Common standard Spring Data JPA method signatures & convenience lookups

    List<Person> findAllByIsResidentTrue();

    // Complex queries for user account provisioning and person search

    /**
     * Searches persons matching the name prefix query who do not currently have an active user account.
     * Orders matching first names first, then last names, limiting to 20 results.
     *
     * @param query the name search keyword (prefix match)
     * @return a list of up to 20 available {@link Person} entities
     */
    @Query("""
            SELECT p FROM Person p
            WHERE (:query IS NULL OR :query = '' OR
                   LOWER(p.firstName) LIKE LOWER(CONCAT(:query, '%')) OR
                   LOWER(p.lastName) LIKE LOWER(CONCAT(:query, '%')) OR
                   LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT(:query, '%')))
              AND p.id NOT IN (
                   SELECT u.person.id FROM User u
                   WHERE u.person.id IS NOT NULL
                     AND (u.isPermanentDeleted IS NULL OR u.isPermanentDeleted = false)
              )
            ORDER BY
                CASE
                    WHEN :query IS NOT NULL AND :query != '' AND LOWER(p.firstName) LIKE LOWER(CONCAT(:query, '%')) THEN 1
                    WHEN :query IS NOT NULL AND :query != '' AND LOWER(p.lastName) LIKE LOWER(CONCAT(:query, '%')) THEN 2
                    ELSE 3
                END,
                p.lastName ASC, p.firstName ASC
            LIMIT 20
            """)
    List<Person> findPersonsWithoutUserAccount(@Param("query") String query);
}
