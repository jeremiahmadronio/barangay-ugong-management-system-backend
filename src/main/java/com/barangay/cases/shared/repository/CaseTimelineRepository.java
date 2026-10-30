package com.barangay.cases.shared.repository;

import com.barangay.cases.shared.model.CaseTimeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseTimelineRepository extends JpaRepository<CaseTimeline, Long> {
    List<CaseTimeline> findByCasesIdOrderByEventDateDescIdDesc(Long caseId);
}
