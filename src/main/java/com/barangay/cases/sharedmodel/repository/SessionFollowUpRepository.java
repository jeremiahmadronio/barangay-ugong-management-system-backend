package com.barangay.cases.sharedmodel.repository;

import com.barangay.cases.sharedmodel.SessionFollowUp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionFollowUpRepository extends JpaRepository<SessionFollowUp, Long> {
}
