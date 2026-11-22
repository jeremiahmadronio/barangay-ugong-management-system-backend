package com.barangay.cases.vawc.repository;

import com.barangay.cases.vawc.model.InterventionFollowUp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for managing VAWC intervention follow-up records.
 */
@Repository
public interface InterventionFollowUpRepository extends JpaRepository<InterventionFollowUp, Long> {

}
