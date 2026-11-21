package com.barangay.clearance.repository;

import com.barangay.clearance.model.CertificateTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateTemplateRepository extends JpaRepository<CertificateTemplate, Long> {

    Optional<CertificateTemplate> findByCertTitle(String certTitle);

    List<CertificateTemplate> findAllByIsArchivedFalseAndIsDeletedFalse();
}
