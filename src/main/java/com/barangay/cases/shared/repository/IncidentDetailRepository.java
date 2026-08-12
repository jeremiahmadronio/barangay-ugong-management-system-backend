package com.barangay.cases.shared.repository;

import com.barangay.cases.shared.model.IncidentDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidentDetailRepository extends JpaRepository<IncidentDetail, Long> {
}
