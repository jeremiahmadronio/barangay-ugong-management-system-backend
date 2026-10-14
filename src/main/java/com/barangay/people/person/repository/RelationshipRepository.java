package com.barangay.people.person.repository;

import com.barangay.people.person.model.Relationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RelationshipRepository extends JpaRepository<Relationship, Long> {
    List<Relationship> findByPersonId(Long personId);
    boolean existsByPersonIdAndRelativeId(Long personId, Long relativeId);
    Optional<Relationship> findByPersonIdAndRelativeId(Long personId, Long relativeId);
    void deleteByPersonIdAndRelativeId(Long personId, Long relativeId);
}

