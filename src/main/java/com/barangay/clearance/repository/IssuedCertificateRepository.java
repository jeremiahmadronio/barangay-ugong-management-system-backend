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

    /** Aggregate KPI metrics for Clearance Reports */
    @Query("""
        SELECT
            COUNT(ic.id) AS totalIssued,
            SUM(CASE WHEN (ic.expiryDate IS NOT NULL AND ic.expiryDate < CURRENT_DATE) OR ic.status = com.barangay.zshared.enumerated.ClearanceStatus.EXPIRED THEN 1L ELSE 0L END) AS totalExpired,
            SUM(CASE WHEN (p IS NOT NULL AND LOWER(p.gender) LIKE 'm%') THEN 1L ELSE 0L END) AS maleCount,
            SUM(CASE WHEN (p IS NOT NULL AND LOWER(p.gender) LIKE 'f%') THEN 1L ELSE 0L END) AS femaleCount
        FROM IssuedCertificate ic
        LEFT JOIN ic.person p
        WHERE ic.createdAt >= :startDate AND ic.createdAt <= :endDate
          AND (ic.isDeleted IS NULL OR ic.isDeleted = false)
          AND (ic.isPermanentDeleted IS NULL OR ic.isPermanentDeleted = false)
          AND (:templateId IS NULL OR ic.template.id = :templateId)
    """)
    com.barangay.clearance.projection.ClearanceReportKpiProjection getClearanceReportKpiMetrics(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("templateId") Long templateId
    );

    /** Most issued certificate template in date range */
    @Query("""
        SELECT t.certTitle AS certTitle, COUNT(ic.id) AS count
        FROM IssuedCertificate ic
        JOIN ic.template t
        WHERE ic.createdAt >= :startDate AND ic.createdAt <= :endDate
          AND (ic.isDeleted IS NULL OR ic.isDeleted = false)
          AND (ic.isPermanentDeleted IS NULL OR ic.isPermanentDeleted = false)
          AND (:templateId IS NULL OR t.id = :templateId)
        GROUP BY t.certTitle
        ORDER BY COUNT(ic.id) DESC
    """)
    List<com.barangay.clearance.projection.MostIssuedCertProjection> getMostIssuedTemplate(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("templateId") Long templateId
    );

    /** Daily issuance counts for trend charts */
    @Query("""
        SELECT CAST(ic.createdAt AS LocalDate) AS date, COUNT(ic.id) AS count
        FROM IssuedCertificate ic
        WHERE ic.createdAt >= :startDate AND ic.createdAt <= :endDate
          AND (ic.isDeleted IS NULL OR ic.isDeleted = false)
          AND (ic.isPermanentDeleted IS NULL OR ic.isPermanentDeleted = false)
          AND (:templateId IS NULL OR ic.template.id = :templateId)
        GROUP BY CAST(ic.createdAt AS LocalDate)
        ORDER BY CAST(ic.createdAt AS LocalDate) ASC
    """)
    List<com.barangay.clearance.projection.ClearanceDailyTrendProjection> getDailyIssuanceCounts(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("templateId") Long templateId
    );

    /** Monthly issuance counts for trend charts */
    @Query("""
        SELECT
            EXTRACT(YEAR FROM ic.createdAt) AS year,
            EXTRACT(MONTH FROM ic.createdAt) AS month,
            COUNT(ic.id) AS count
        FROM IssuedCertificate ic
        WHERE ic.createdAt >= :startDate AND ic.createdAt <= :endDate
          AND (ic.isDeleted IS NULL OR ic.isDeleted = false)
          AND (ic.isPermanentDeleted IS NULL OR ic.isPermanentDeleted = false)
          AND (:templateId IS NULL OR ic.template.id = :templateId)
        GROUP BY EXTRACT(YEAR FROM ic.createdAt), EXTRACT(MONTH FROM ic.createdAt)
        ORDER BY EXTRACT(YEAR FROM ic.createdAt) ASC, EXTRACT(MONTH FROM ic.createdAt) ASC
    """)
    List<com.barangay.clearance.projection.ClearanceMonthlyTrendProjection> getMonthlyIssuanceCounts(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("templateId") Long templateId
    );

    /** Aggregate certificate count breakdown per template */
    @Query("""
        SELECT t.certTitle AS certTitle, COUNT(ic.id) AS count
        FROM IssuedCertificate ic
        JOIN ic.template t
        WHERE ic.createdAt >= :startDate AND ic.createdAt <= :endDate
          AND (ic.isDeleted IS NULL OR ic.isDeleted = false)
          AND (ic.isPermanentDeleted IS NULL OR ic.isPermanentDeleted = false)
        GROUP BY t.certTitle
        ORDER BY COUNT(ic.id) DESC
    """)
    List<com.barangay.clearance.projection.ClearanceTypeCountProjection> getTypeBreakdownCounts(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /** Fetch active certificates within date range for purpose aggregation */
    @Query("""
        SELECT ic FROM IssuedCertificate ic
        LEFT JOIN FETCH ic.template t
        WHERE ic.createdAt >= :startDate AND ic.createdAt <= :endDate
          AND (ic.isDeleted IS NULL OR ic.isDeleted = false)
          AND (ic.isPermanentDeleted IS NULL OR ic.isPermanentDeleted = false)
        ORDER BY ic.createdAt DESC
    """)
    List<IssuedCertificate> findCertificatesInDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Aggregates lifecycle counts for Clearance records (Archived and Recently Deleted).
     *
     * @param startOfMonth start timestamp of the current month
     * @param endOfMonth   end timestamp of the current month
     * @return {@link com.barangay.clearance.projection.ClearanceLifecycleKpiProjection}
     */
    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN ic.isArchived = true AND (ic.isDeleted IS NULL OR ic.isDeleted = false) THEN 1L ELSE 0L END), 0L) AS totalArchived,
            COALESCE(SUM(CASE WHEN ic.isArchived = true AND (ic.isDeleted IS NULL OR ic.isDeleted = false)
                     AND ((ic.archivedAt IS NOT NULL AND ic.archivedAt >= :startOfMonth AND ic.archivedAt <= :endOfMonth)
                          OR (ic.archivedAt IS NULL AND ic.updatedAt >= :startOfMonth AND ic.updatedAt <= :endOfMonth)) THEN 1L ELSE 0L END), 0L) AS totalArchivedThisMonth,
            COALESCE(SUM(CASE WHEN ic.isDeleted = true AND (ic.isPermanentDeleted IS NULL OR ic.isPermanentDeleted = false) THEN 1L ELSE 0L END), 0L) AS totalRecentlyDeleted,
            COALESCE(SUM(CASE WHEN ((ic.isDeleted = true AND ic.deletedAt IS NOT NULL AND ic.deletedAt >= :startOfMonth AND ic.deletedAt <= :endOfMonth)
                          OR (ic.isPermanentDeleted = true AND ic.permanentDeletedAt IS NOT NULL AND ic.permanentDeletedAt >= :startOfMonth AND ic.permanentDeletedAt <= :endOfMonth)
                          OR ((ic.isDeleted = true OR ic.isPermanentDeleted = true) AND ic.updatedAt >= :startOfMonth AND ic.updatedAt <= :endOfMonth)) THEN 1L ELSE 0L END), 0L) AS totalRecentlyDeletedThisMonth
        FROM IssuedCertificate ic
    """)
    com.barangay.clearance.projection.ClearanceLifecycleKpiProjection getClearanceLifecycleKpiMetrics(
        @Param("startOfMonth") LocalDateTime startOfMonth,
        @Param("endOfMonth") LocalDateTime endOfMonth
    );

    /**
     * Retrieves paginated active archived certificates with optional search filter.
     */
    @Query(
        value = """
            SELECT ic FROM IssuedCertificate ic
            LEFT JOIN FETCH ic.template t
            LEFT JOIN FETCH ic.person p
            WHERE ic.isArchived = true
              AND (ic.isDeleted IS NULL OR ic.isDeleted = false)
              AND (ic.isPermanentDeleted IS NULL OR ic.isPermanentDeleted = false)
              AND (:search IS NULL OR :search = ''
                   OR LOWER(ic.certNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(ic.requestorName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(t.certTitle) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY ic.archivedAt DESC, ic.updatedAt DESC
        """,
        countQuery = """
            SELECT COUNT(ic) FROM IssuedCertificate ic
            LEFT JOIN ic.template t
            LEFT JOIN ic.person p
            WHERE ic.isArchived = true
              AND (ic.isDeleted IS NULL OR ic.isDeleted = false)
              AND (ic.isPermanentDeleted IS NULL OR ic.isPermanentDeleted = false)
              AND (:search IS NULL OR :search = ''
                   OR LOWER(ic.certNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(ic.requestorName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(t.certTitle) LIKE LOWER(CONCAT('%', :search, '%')))
        """
    )
    org.springframework.data.domain.Page<IssuedCertificate> findArchivedCertificates(
        @Param("search") String search,
        org.springframework.data.domain.Pageable pageable
    );

    /**
     * Retrieves paginated soft-deleted certificates in trash with optional search filter.
     */
    @Query(
        value = """
            SELECT ic FROM IssuedCertificate ic
            LEFT JOIN FETCH ic.template t
            LEFT JOIN FETCH ic.person p
            WHERE ic.isDeleted = true
              AND (ic.isPermanentDeleted IS NULL OR ic.isPermanentDeleted = false)
              AND (:search IS NULL OR :search = ''
                   OR LOWER(ic.certNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(ic.requestorName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(t.certTitle) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY ic.deletedAt DESC, ic.updatedAt DESC
        """,
        countQuery = """
            SELECT COUNT(ic) FROM IssuedCertificate ic
            LEFT JOIN ic.template t
            LEFT JOIN ic.person p
            WHERE ic.isDeleted = true
              AND (ic.isPermanentDeleted IS NULL OR ic.isPermanentDeleted = false)
              AND (:search IS NULL OR :search = ''
                   OR LOWER(ic.certNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(ic.requestorName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(t.certTitle) LIKE LOWER(CONCAT('%', :search, '%')))
        """
    )
    org.springframework.data.domain.Page<IssuedCertificate> findRecentDeletedCertificates(
        @Param("search") String search,
        org.springframework.data.domain.Pageable pageable
    );

    /**
     * Finds all soft-deleted certificates that have exceeded the retention threshold.
     *
     * @param cutoff Date threshold before which soft-deleted records should be permanently deleted.
     * @return List of expired soft-deleted certificates
     */
    @Query("""
        SELECT ic FROM IssuedCertificate ic
        WHERE ic.isDeleted = true
          AND (ic.isPermanentDeleted IS NULL OR ic.isPermanentDeleted = false)
          AND (
              (ic.deletedAt IS NOT NULL AND ic.deletedAt <= :cutoff)
              OR (ic.deletedAt IS NULL AND ic.updatedAt <= :cutoff)
          )
    """)
    List<IssuedCertificate> findExpiredSoftDeletedCertificates(@Param("cutoff") LocalDateTime cutoff);
}

