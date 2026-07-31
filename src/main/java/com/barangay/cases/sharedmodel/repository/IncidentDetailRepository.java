package com.barangay.cases.sharedmodel.repository;

import com.barangay.cases.sharedmodel.IncidentDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidentDetailRepository extends JpaRepository<IncidentDetail, Long> {
}
