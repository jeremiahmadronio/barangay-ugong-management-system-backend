package com.barangay.cases.sharedmodel.repository;

import com.barangay.cases.sharedmodel.EvidenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EvidenceTypeRepository extends JpaRepository<EvidenceType, Long> {

    Optional<EvidenceType> findByNameIgnoreCase(String name);
}
