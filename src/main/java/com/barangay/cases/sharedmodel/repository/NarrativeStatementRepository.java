package com.barangay.cases.sharedmodel.repository;

import com.barangay.cases.sharedmodel.NarrativeStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NarrativeStatementRepository extends JpaRepository<NarrativeStatement, Long> {
}
