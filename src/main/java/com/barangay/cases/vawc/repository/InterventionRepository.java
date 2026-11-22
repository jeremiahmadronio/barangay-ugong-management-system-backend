package com.barangay.cases.vawc.repository;

import com.barangay.cases.vawc.model.Intervention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for managing VAWC case intervention activities.
 */
@Repository
public interface InterventionRepository extends JpaRepository<Intervention, Long> {
    List<Intervention> findByCasesId(Long caseId);
}
