package com.barangay.cases.shared.repository;

import com.barangay.cases.shared.model.SessionMinutes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SessionMinutesRepository extends JpaRepository<SessionMinutes, Long> {
    Optional<SessionMinutes> findFirstBySessionIdOrderByCreatedAtDesc(Long sessionId);
}

