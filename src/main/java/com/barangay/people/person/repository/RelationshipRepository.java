package com.barangay.people.person.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.barangay.people.person.model.Relationship;

/**
 * Spring Data JPA repository for managing bidirectional family relationships between persons.
 */
@Repository
public interface RelationshipRepository extends JpaRepository<Relationship, Long> {

    // Common standard Spring Data JPA method signatures & convenience lookups

    List<Relationship> findByPersonId(Long personId);

    boolean existsByPersonIdAndRelativeId(Long personId, Long relativeId);

    Optional<Relationship> findByPersonIdAndRelativeId(Long personId, Long relativeId);

    void deleteByPersonIdAndRelativeId(Long personId, Long relativeId);
}
