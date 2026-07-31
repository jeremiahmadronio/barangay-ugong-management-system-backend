package com.barangay.cases.sharedmodel.repository;

import com.barangay.cases.sharedmodel.CaseSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CaseSessionRepository extends JpaRepository<CaseSession, Long> {
}
