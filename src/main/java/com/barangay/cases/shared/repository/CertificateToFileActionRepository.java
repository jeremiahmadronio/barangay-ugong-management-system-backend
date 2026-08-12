package com.barangay.cases.shared.repository;

import com.barangay.cases.shared.model.CertificateToFileAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CertificateToFileActionRepository extends JpaRepository<CertificateToFileAction, Long> {
}
