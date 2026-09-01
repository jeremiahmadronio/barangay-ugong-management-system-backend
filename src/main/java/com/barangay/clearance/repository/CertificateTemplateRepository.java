package com.barangay.clearance.repository;

import com.barangay.clearance.model.CertificateTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link CertificateTemplate}.
 */
@Repository
public interface CertificateTemplateRepository extends JpaRepository<CertificateTemplate, Long>, JpaSpecificationExecutor<CertificateTemplate> {

    /** Find a template by its exact certificate title. */
    Optional<CertificateTemplate> findByCertTitle(String certTitle);

    /** Check if a template with the given title already exists. */
    boolean existsByCertTitle(String certTitle);

    /** Retrieve all active (non-archived and non-deleted) templates. */
    List<CertificateTemplate> findAllByIsArchivedFalseAndIsDeletedFalse();
}
