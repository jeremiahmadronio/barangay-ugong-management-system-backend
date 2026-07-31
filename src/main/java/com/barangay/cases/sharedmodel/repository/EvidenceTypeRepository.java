package com.barangay.cases.sharedmodel.repository;

import com.barangay.cases.sharedmodel.EvidenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvidenceTypeRepository extends JpaRepository<EvidenceType, Long> {
}
