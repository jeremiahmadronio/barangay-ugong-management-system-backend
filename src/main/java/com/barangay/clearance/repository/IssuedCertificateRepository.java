package com.barangay.clearance.repository;

import com.barangay.clearance.model.IssuedCertificate;
import com.barangay.zshared.enumerated.ClearanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    /** Find all active, non-deleted issued certificates for a specific person with fetched template. */
    @Query("""
        SELECT ic FROM IssuedCertificate ic
        LEFT JOIN FETCH ic.template t
        WHERE (ic.person.id = :personId OR LOWER(TRIM(ic.requestorName)) = LOWER(TRIM(:fullName)))
          AND (ic.isDeleted IS NULL OR ic.isDeleted = false)
          AND (ic.isPermanentDeleted IS NULL OR ic.isPermanentDeleted = false)
        ORDER BY ic.createdAt DESC
    """)
    List<IssuedCertificate> findActiveCertificatesByPersonOrName(
        @Param("personId") Long personId,
        @Param("fullName") String fullName
    );

    @Query("""
        SELECT ic FROM IssuedCertificate ic
        LEFT JOIN FETCH ic.template t
        WHERE ic.person.id = :personId
          AND (ic.isDeleted IS NULL OR ic.isDeleted = false)
          AND (ic.isPermanentDeleted IS NULL OR ic.isPermanentDeleted = false)
        ORDER BY ic.createdAt DESC
    """)
    List<IssuedCertificate> findActiveCertificatesByPersonId(@Param("personId") Long personId);

    /** Count active, non-archived, non-deleted certificates */
    @Query("""
        SELECT COUNT(ic) FROM IssuedCertificate ic
        WHERE (ic.isArchived IS NULL OR ic.isArchived = false)
          AND (ic.isDeleted IS NULL OR ic.isDeleted = false)
          AND (ic.isPermanentDeleted IS NULL OR ic.isPermanentDeleted = false)
    """)
    long countActiveCertificates();

    /** Count active certificates created within a date range */
    @Query("""
        SELECT COUNT(ic) FROM IssuedCertificate ic
        WHERE ic.createdAt >= :startDate AND ic.createdAt <= :endDate
          AND (ic.isArchived IS NULL OR ic.isArchived = false)
          AND (ic.isDeleted IS NULL OR ic.isDeleted = false)
          AND (ic.isPermanentDeleted IS NULL OR ic.isPermanentDeleted = false)
    """)
    long countActiveCertificatesBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /** Get active certificates for statistical analysis */
    @Query("""
        SELECT ic FROM IssuedCertificate ic
        LEFT JOIN FETCH ic.template t
        WHERE (ic.isArchived IS NULL OR ic.isArchived = false)
          AND (ic.isDeleted IS NULL OR ic.isDeleted = false)
          AND (ic.isPermanentDeleted IS NULL OR ic.isPermanentDeleted = false)
    """)
    List<IssuedCertificate> findAllActiveCertificates();

    /** Find active issued certificates with prefix search and template filtering */
    @Query("""
        SELECT ic FROM IssuedCertificate ic
        LEFT JOIN FETCH ic.template t
        LEFT JOIN FETCH ic.person p
        WHERE (ic.isArchived IS NULL OR ic.isArchived = false)
          AND (ic.isDeleted IS NULL OR ic.isDeleted = false)
          AND (ic.isPermanentDeleted IS NULL OR ic.isPermanentDeleted = false)
          AND (:templateTitle IS NULL OR :templateTitle = '' OR LOWER(t.certTitle) = LOWER(:templateTitle))
          AND (
              :search IS NULL OR :search = '' OR
              LOWER(ic.certNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR
              LOWER(ic.requestorName) LIKE LOWER(CONCAT(:search, '%')) OR
              LOWER(ic.requestorName) LIKE LOWER(CONCAT('% ', :search, '%')) OR
              (p IS NOT NULL AND (
                  LOWER(p.firstName) LIKE LOWER(CONCAT(:search, '%')) OR
                  LOWER(p.lastName) LIKE LOWER(CONCAT(:search, '%'))
              ))
          )
        ORDER BY ic.createdAt DESC
    """)
    List<IssuedCertificate> findActiveIssuedCertificates(
        @Param("search") String search,
        @Param("templateTitle") String templateTitle
    );

    /** Find complete issued certificate detail by ID with all relations fetched */
    @Query("""
        SELECT ic FROM IssuedCertificate ic
        LEFT JOIN FETCH ic.template t
        LEFT JOIN FETCH ic.person p
        LEFT JOIN FETCH ic.issuedBy u
        WHERE ic.id = :id
          AND (ic.isDeleted IS NULL OR ic.isDeleted = false)
          AND (ic.isPermanentDeleted IS NULL OR ic.isPermanentDeleted = false)
    """)
    Optional<IssuedCertificate> findDetailById(@Param("id") Long id);
}

