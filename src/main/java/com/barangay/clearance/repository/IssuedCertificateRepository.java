package com.barangay.clearance.repository;

import com.barangay.clearance.model.IssuedCertificate;
import com.barangay.zshared.enumerated.ClearanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link IssuedCertificate}.
 */
@Repository
public interface IssuedCertificateRepository extends JpaRepository<IssuedCertificate, Long>, JpaSpecificationExecutor<IssuedCertificate> {

    /** Find an issued certificate by its unique certificate number. */
    Optional<IssuedCertificate> findByCertNumber(String certNumber);

    /** Check if a certificate number already exists. */
    boolean existsByCertNumber(String certNumber);

    /** Find all issued certificates for a specific person, ordered by issuance date descending. */
    List<IssuedCertificate> findByPersonIdAndIsDeletedFalseOrderByCreatedAtDesc(Long personId);

    /** Find all issued certificates by status and active audit state with pagination. */
    Page<IssuedCertificate> findByStatusAndIsDeletedFalse(ClearanceStatus status, Pageable pageable);
}
