package com.barangay.people.witness.repository;

import com.barangay.people.witness.model.Witness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface WitnessRepository extends JpaRepository<Witness, Long> {
    @Query("SELECT w FROM Witness w WHERE w.aCase.id = :caseId")
    List<Witness> findByCaseId(@Param("caseId") Long caseId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Witness w WHERE w.aCase.id = :caseId")
    void deleteByACaseId(@Param("caseId") Long caseId);
}
