package com.barangay.cases.sharedmodel.repository;

import com.barangay.cases.sharedmodel.CaseTimeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CaseTimelineRepository extends JpaRepository<CaseTimeline, Long> {
}
