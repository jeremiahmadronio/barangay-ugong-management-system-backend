package com.barangay.cases.shared.repository;

import com.barangay.cases.shared.model.CertificateToFileAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CertificateToFileActionRepository extends JpaRepository<CertificateToFileAction, Long> {
    Optional<CertificateToFileAction> findByCases_Id(Long caseId);
}
