package com.barangay.cases.shared.repository;

import com.barangay.cases.shared.model.SessionFollowUp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionFollowUpRepository extends JpaRepository<SessionFollowUp, Long> {
    List<SessionFollowUp> findBySessionIdOrderByCreatedAtDesc(Long sessionId);
}

