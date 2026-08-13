package com.barangay.ftjs.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.barangay.ftjs.model.FirstTimeJobSeeker;
import com.barangay.zshared.enumerated.FtjsStatus;

@Repository
public interface FirstTimeJobSeekerRepository extends JpaRepository<FirstTimeJobSeeker, Long> {

    /**
     * Efficient indexed lookup for resident issuance history.
     */
    @Query("SELECT f FROM FirstTimeJobSeeker f WHERE f.resident.residentId = :residentId AND f.isDeleted = false ORDER BY f.createdAt DESC")
    List<FirstTimeJobSeeker> findByResidentIdAndIsDeletedFalseOrderByCreatedAtDesc(@Param("residentId") Long residentId);

    @Query("SELECT f FROM FirstTimeJobSeeker f WHERE f.person.id = :personId AND f.isDeleted = false ORDER BY f.createdAt DESC")
    List<FirstTimeJobSeeker> findByPersonIdAndIsDeletedFalseOrderByCreatedAtDesc(@Param("personId") Long personId);

    /**
     * Fast duplicate check under RA 11261.
     */
    @Query("SELECT COUNT(f) > 0 FROM FirstTimeJobSeeker f WHERE f.resident.residentId = :residentId AND f.isDeleted = false")
    boolean existsByResidentIdAndIsDeletedFalse(@Param("residentId") Long residentId);





    // ── Stats queries ─────────────────────────────────────────────────────────

    @Query("SELECT COUNT(f) FROM FirstTimeJobSeeker f WHERE f.isDeleted = false AND f.isArchived = false AND f.status IN (com.barangay.zshared.enumerated.FtjsStatus.ISSUED, com.barangay.zshared.enumerated.FtjsStatus.RE_ISSUANCE)")
    long countTotalIssued();

    @Query("SELECT COUNT(f) FROM FirstTimeJobSeeker f WHERE f.isDeleted = false AND f.dateSubmitted BETWEEN :start AND :end AND f.status IN (com.barangay.zshared.enumerated.FtjsStatus.ISSUED, com.barangay.zshared.enumerated.FtjsStatus.RE_ISSUANCE)")
    long countIssuedBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COUNT(f) FROM FirstTimeJobSeeker f WHERE f.isDeleted = false AND f.isArchived = false AND (f.issuanceCount IS NULL OR f.issuanceCount <= 1)")
    long countOriginalIssuances();

    @Query("SELECT COUNT(f) FROM FirstTimeJobSeeker f WHERE f.isDeleted = false AND f.isArchived = false AND f.issuanceCount > 1")
    long countReIssuances();

    // ── Paginated summary table with filtering ────────────────────────────────

    @Query("SELECT new com.barangay.ftjs.dto.FtjsSummaryResponse(" +
            "f.id, " +
            "f.ftjsNumber, " +
            "CONCAT(f.person.firstName, ' ', COALESCE(CONCAT(f.person.middleName, ' '), ''), f.person.lastName), " +
            "CAST(f.issuanceCount AS long), " +
            "f.status, " +
            "f.purposeDocuments, " +
            "f.createdAt) " +
            "FROM FirstTimeJobSeeker f " +
            "WHERE f.isDeleted = false AND f.isArchived = false " +
            "AND (:search IS NULL OR :search = '' " +
            "  OR LOWER(f.ftjsNumber) LIKE LOWER(CONCAT(:search, '%')) " +
            "  OR LOWER(f.person.firstName) LIKE LOWER(CONCAT(:search, '%')) " +
            "  OR LOWER(f.person.lastName) LIKE LOWER(CONCAT(:search, '%'))) " +
            "AND (:status IS NULL OR f.status = :status) " +
            "AND (:purpose IS NULL OR :purpose = '' OR f.purposeDocuments = :purpose) " +
            "AND (:issuanceCount IS NULL OR f.issuanceCount = :issuanceCount)")
    Page<com.barangay.ftjs.dto.FtjsSummaryResponse> findSummaryFiltered(
            @Param("search") String search,
            @Param("status") FtjsStatus status,
            @Param("purpose") String purpose,
            @Param("issuanceCount") Integer issuanceCount,
            Pageable pageable);
}
