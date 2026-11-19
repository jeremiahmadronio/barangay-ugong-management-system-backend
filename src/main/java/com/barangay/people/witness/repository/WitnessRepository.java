package com.barangay.people.witness.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.barangay.people.witness.model.Witness;

/**
 * Spring Data JPA repository for managing case witnesses and associated testimonies.
 */
@Repository
public interface WitnessRepository extends JpaRepository<Witness, Long> {

    // Common standard Spring Data JPA method signatures & convenience lookups

    List<Witness> findAllByPersonId(Long personId);

    // Case-linked queries with Fetch Join to prevent N+1 queries when reading witness profiles

    /**
     * Finds all witnesses associated with a specific case ID, fetching linked person details.
     *
     * @param caseId the ID of the case
     * @return a list of {@link Witness} entities with initialized person records
     */
    @Query("SELECT w FROM Witness w LEFT JOIN FETCH w.person p WHERE w.aCase.id = :caseId")
    List<Witness> findByCaseId(@Param("caseId") Long caseId);

    /**
     * Deletes all witness testimonies linked to a specific case ID.
     *
     * @param caseId the ID of the case to clean up
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Witness w WHERE w.aCase.id = :caseId")
    void deleteByACaseId(@Param("caseId") Long caseId);
}
