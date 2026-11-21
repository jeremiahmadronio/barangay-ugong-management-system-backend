package com.barangay.clearance.repository;

import com.barangay.clearance.model.IssuedCertificate;
import com.barangay.clearance.projection.ClearanceDailyTrendProjection;
import com.barangay.clearance.projection.ClearanceLifecycleKpiProjection;
import com.barangay.clearance.projection.ClearanceMonthlyTrendProjection;
import com.barangay.clearance.projection.ClearanceReportKpiProjection;
import com.barangay.clearance.projection.ClearanceTypeCountProjection;
import com.barangay.clearance.projection.MostIssuedCertProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IssuedCertificateRepository extends JpaRepository<IssuedCertificate, Long> {

    boolean existsByCertNumber(String certNumber);

    @Query("SELECT COUNT(ic) FROM IssuedCertificate ic WHERE ic.createdAt >= :start AND ic.createdAt <= :end AND (ic.isArchived = false OR ic.isArchived IS NULL) AND (ic.isDeleted = false OR ic.isDeleted IS NULL) AND (ic.isPermanentDeleted = false OR ic.isPermanentDeleted IS NULL)")
    long countWeeklyIssuedCertificates(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(ic) FROM IssuedCertificate ic WHERE (ic.isArchived IS NULL OR ic.isArchived = false) AND (ic.isDeleted IS NULL OR ic.isDeleted = false) AND (ic.isPermanentDeleted IS NULL OR ic.isPermanentDeleted = false)")
    long countActiveCertificates();

    @Query("SELECT COUNT(ic) FROM IssuedCertificate ic WHERE ic.createdAt >= :startDate AND ic.createdAt <= :endDate AND (ic.isArchived IS NULL OR ic.isArchived = false) AND (ic.isDeleted IS NULL OR ic.isDeleted = false) AND (ic.isPermanentDeleted IS NULL OR ic.isPermanentDeleted = false)")
    long countActiveCertificatesBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT ic FROM IssuedCertificate ic LEFT JOIN FETCH ic.template t WHERE (ic.isArchived IS NULL OR ic.isArchived = false) AND (ic.isDeleted IS NULL OR ic.isDeleted = false) AND (ic.isPermanentDeleted IS NULL OR ic.isPermanentDeleted = false)")
    List<IssuedCertificate> findAllActiveCertificates();

    @Query("SELECT ic FROM IssuedCertificate ic LEFT JOIN FETCH ic.template t WHERE ic.person.id = :personId AND (ic.isDeleted IS NULL OR ic.isDeleted = false) AND (ic.isPermanentDeleted IS NULL OR ic.isPermanentDeleted = false) ORDER BY ic.createdAt DESC")
    List<IssuedCertificate> findActiveCertificatesByPersonId(@Param("personId") Long personId);

    @Query("SELECT ic FROM IssuedCertificate ic LEFT JOIN FETCH ic.template t WHERE ic.createdAt >= :startDate AND ic.createdAt <= :endDate AND (ic.isDeleted IS NULL OR ic.isDeleted = false) AND (ic.isPermanentDeleted IS NULL OR ic.isPermanentDeleted = false) ORDER BY ic.createdAt DESC")
    List<IssuedCertificate> findCertificatesInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT ic FROM IssuedCertificate ic LEFT JOIN FETCH ic.template t LEFT JOIN FETCH ic.person p LEFT JOIN FETCH ic.issuedBy u WHERE ic.id = :id AND (ic.isDeleted IS NULL OR ic.isDeleted = false) AND (ic.isPermanentDeleted IS NULL OR ic.isPermanentDeleted = false)")
    Optional<IssuedCertificate> findDetailById(@Param("id") Long id);

    @Query("SELECT ic FROM IssuedCertificate ic LEFT JOIN FETCH ic.template t WHERE (ic.person.id = :personId OR LOWER(TRIM(ic.requestorName)) = LOWER(TRIM(:fullName))) AND (ic.isDeleted IS NULL OR ic.isDeleted = false) AND (ic.isPermanentDeleted IS NULL OR ic.isPermanentDeleted = false) ORDER BY ic.createdAt DESC")
    List<IssuedCertificate> findActiveCertificatesByPersonOrName(@Param("personId") Long personId, @Param("fullName") String fullName);

    @Query("SELECT ic FROM IssuedCertificate ic LEFT JOIN FETCH ic.template t WHERE (ic.person.id = :personId OR LOWER(TRIM(ic.requestorName)) = LOWER(TRIM(:fullName))) ORDER BY ic.createdAt DESC")
    List<IssuedCertificate> findAllCertificatesByPersonOrName(@Param("personId") Long personId, @Param("fullName") String fullName);

    @Query("SELECT t.certTitle AS certTitle, COUNT(ic.id) AS count FROM IssuedCertificate ic JOIN ic.template t WHERE ic.createdAt >= :startDate AND ic.createdAt <= :endDate AND (ic.isDeleted IS NULL OR ic.isDeleted = false) AND (ic.isPermanentDeleted IS NULL OR ic.isPermanentDeleted = false) GROUP BY t.certTitle ORDER BY COUNT(ic.id) DESC")
    List<ClearanceTypeCountProjection> getTypeBreakdownCounts(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t.certTitle AS certTitle, COUNT(ic.id) AS count FROM IssuedCertificate ic JOIN ic.template t WHERE ic.createdAt >= :startDate AND ic.createdAt <= :endDate AND (ic.isDeleted IS NULL OR ic.isDeleted = false) AND (ic.isPermanentDeleted IS NULL OR ic.isPermanentDeleted = false) AND (:templateId IS NULL OR t.id = :templateId) GROUP BY t.certTitle ORDER BY COUNT(ic.id) DESC")
    List<MostIssuedCertProjection> getMostIssuedTemplate(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("templateId") Long templateId);

    @Query("SELECT CAST(ic.createdAt AS LocalDate) AS date, COUNT(ic.id) AS count FROM IssuedCertificate ic WHERE ic.createdAt >= :startDate AND ic.createdAt <= :endDate AND (ic.isDeleted IS NULL OR ic.isDeleted = false) AND (ic.isPermanentDeleted IS NULL OR ic.isPermanentDeleted = false) AND (:templateId IS NULL OR ic.template.id = :templateId) GROUP BY CAST(ic.createdAt AS LocalDate) ORDER BY CAST(ic.createdAt AS LocalDate) ASC")
    List<ClearanceDailyTrendProjection> getDailyIssuanceCounts(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("templateId") Long templateId);

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
    List<ClearanceMonthlyTrendProjection> getMonthlyIssuanceCounts(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("templateId") Long templateId);

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
    ClearanceReportKpiProjection getClearanceReportKpiMetrics(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("templateId") Long templateId);

    @Query("""
        SELECT ic FROM IssuedCertificate ic
        LEFT JOIN FETCH ic.template t
        LEFT JOIN FETCH ic.person p
        LEFT JOIN FETCH ic.issuedBy u
        LEFT JOIN FETCH u.person up
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
    List<IssuedCertificate> findActiveIssuedCertificates(@Param("search") String search, @Param("templateTitle") String templateTitle);

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
    ClearanceLifecycleKpiProjection getClearanceLifecycleKpiMetrics(@Param("startOfMonth") LocalDateTime startOfMonth, @Param("endOfMonth") LocalDateTime endOfMonth);

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
    Page<IssuedCertificate> findArchivedCertificates(@Param("search") String search, Pageable pageable);

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
    Page<IssuedCertificate> findRecentDeletedCertificates(@Param("search") String search, Pageable pageable);
}
