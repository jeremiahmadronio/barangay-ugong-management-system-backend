package com.barangay.cases.shared.repository;

import com.barangay.cases.shared.model.NarrativeStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NarrativeStatementRepository extends JpaRepository<NarrativeStatement, Long> {
}
