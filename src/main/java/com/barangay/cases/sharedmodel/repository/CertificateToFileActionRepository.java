package com.barangay.cases.sharedmodel.repository;

import com.barangay.cases.sharedmodel.CertificateToFileAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CertificateToFileActionRepository extends JpaRepository<CertificateToFileAction, Long> {
}
