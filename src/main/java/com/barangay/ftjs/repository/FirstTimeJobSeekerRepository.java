package com.barangay.ftjs.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.barangay.ftjs.dto.FtjsSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.barangay.ftjs.model.FirstTimeJobSeeker;
import com.barangay.zshared.enumerated.FtjsStatus;

@Repository
public interface FirstTimeJobSeekerRepository extends JpaRepository<FirstTimeJobSeeker, Long> {

    @Modifying
    @Query(value = "UPDATE first_time_job_seeker SET is_archived = true, is_deleted = false, deleted_at = NULL, archived_at = NOW() WHERE id = :id", nativeQuery = true)
    int archiveRecordNative(@Param("id") Long id);

    @Modifying
    @Query(value = "UPDATE first_time_job_seeker SET is_archived = false, is_deleted = false, deleted_at = NULL, archived_at = NULL WHERE id = :id", nativeQuery = true)
    int restoreRecordNative(@Param("id") Long id);

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

    @Query("SELECT COUNT(f) FROM FirstTimeJobSeeker f WHERE f.isDeleted = false AND f.status IN (com.barangay.zshared.enumerated.FtjsStatus.ISSUED, com.barangay.zshared.enumerated.FtjsStatus.RE_ISSUANCE)")
    long countTotalIssued();

    @Query("SELECT COUNT(f) FROM FirstTimeJobSeeker f WHERE f.isDeleted = false AND f.dateSubmitted BETWEEN :start AND :end AND f.status IN (com.barangay.zshared.enumerated.FtjsStatus.ISSUED, com.barangay.zshared.enumerated.FtjsStatus.RE_ISSUANCE)")
    long countIssuedBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COUNT(f) FROM FirstTimeJobSeeker f WHERE f.isDeleted = false AND (f.issuanceCount IS NULL OR f.issuanceCount <= 1)")
    long countOriginalIssuances();

    @Query("SELECT COUNT(f) FROM FirstTimeJobSeeker f WHERE f.isDeleted = false AND f.issuanceCount > 1")
    long countReIssuances();


    long countByIsArchivedTrue();

    @Query("SELECT COUNT(f) FROM FirstTimeJobSeeker f WHERE f.isArchived = true AND f.archivedAt BETWEEN :start AND :end")
    long countArchiveBetween(
         @Param("start")  LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT COUNT(f) from FirstTimeJobSeeker f WHERE f.isArchived = true AND (f.issuanceCount = 1 OR f.issuanceCount IS NULL)")
    long countArchiveOriginalIssuance();

    @Query(value = "SELECT COUNT(*) from  first_time_job_seeker  WHERE is_deleted  = true",nativeQuery = true)
    long countTotalRecentDelete();

    @Modifying
    @Query(value = "DELETE FROM first_time_job_seeker_affidavit_of_loss WHERE ftjs_id = :id", nativeQuery = true)
    int deleteChildAffidavits(@Param("id") Long id);

    @Modifying
    @Query(value = "DELETE FROM first_time_job_seeker_timeline WHERE ftjs_id = :id", nativeQuery = true)
    int deleteChildTimelines(@Param("id") Long id);

    @Modifying
    @Query(value = "DELETE FROM first_time_job_seeker_notes WHERE ftjs_id = :id", nativeQuery = true)
    int deleteChildNotes(@Param("id") Long id);

    @Modifying
    @Query(value = "DELETE FROM first_time_job_seeker_affidavit_of_loss WHERE ftjs_id IN (:ids)", nativeQuery = true)
    int deleteChildAffidavitsBatch(@Param("ids") List<Long> ids);

    @Modifying
    @Query(value = "DELETE FROM first_time_job_seeker_timeline WHERE ftjs_id IN (:ids)", nativeQuery = true)
    int deleteChildTimelinesBatch(@Param("ids") List<Long> ids);

    @Modifying
    @Query(value = "DELETE FROM first_time_job_seeker_notes WHERE ftjs_id IN (:ids)", nativeQuery = true)
    int deleteChildNotesBatch(@Param("ids") List<Long> ids);

    @Modifying
    @Query(value = "DELETE FROM first_time_job_seeker_affidavit_of_loss WHERE ftjs_id IN (SELECT id FROM first_time_job_seeker WHERE is_deleted = true AND deleted_at <= :cutoffDate)", nativeQuery = true)
    int deleteChildAffidavitsExpired(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying
    @Query(value = "DELETE FROM first_time_job_seeker_timeline WHERE ftjs_id IN (SELECT id FROM first_time_job_seeker WHERE is_deleted = true AND deleted_at <= :cutoffDate)", nativeQuery = true)
    int deleteChildTimelinesExpired(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying
    @Query(value = "DELETE FROM first_time_job_seeker_notes WHERE ftjs_id IN (SELECT id FROM first_time_job_seeker WHERE is_deleted = true AND deleted_at <= :cutoffDate)", nativeQuery = true)
    int deleteChildNotesExpired(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying
    @Query(value = "DELETE FROM first_time_job_seeker WHERE is_deleted = true AND deleted_at <= :cutoffDate", nativeQuery = true)
    int hardDeleteExpiredRecords(@Param("cutoffDate") LocalDateTime cutoffDate);


    @Modifying
    @Query(value = "DELETE FROM first_time_job_seeker WHERE id = :id AND is_deleted = true", nativeQuery = true)
    int hardDeleteById(@Param("id") Long id);

    @Modifying
    @Query(value = "DELETE FROM first_time_job_seeker WHERE id IN (:ids) AND is_deleted = true", nativeQuery = true)
    int hardDeleteByIds(@Param("ids") List<Long> ids);





    @Query(value = "SELECT new com.barangay.ftjs.dto.FtjsSummaryResponse(" +
            "f.id, " +
            "f.ftjsNumber, " +
            "CONCAT(COALESCE(p.firstName, ''), ' ', COALESCE(CONCAT(p.middleName, ' '), ''), COALESCE(p.lastName, '')), " +
            "CAST(COALESCE(f.issuanceCount, 1) AS long), " +
            "f.status, " +
            "f.purposeDocuments, " +
            "f.createdAt) " +
            "FROM FirstTimeJobSeeker f " +
            "LEFT JOIN f.person p " +
            "WHERE (f.isDeleted = false OR f.isDeleted IS NULL) AND (f.isArchived = false OR f.isArchived IS NULL) " +
            "AND (:search IS NULL OR :search = '' " +
            "  OR LOWER(f.ftjsNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "  OR LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "  OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "  OR LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR f.status = :status) " +
            "AND (:purpose IS NULL OR :purpose = '' OR f.purposeDocuments = :purpose) " +
            "AND (:issuanceCount IS NULL OR f.issuanceCount = :issuanceCount)",
            countQuery = "SELECT COUNT(f) FROM FirstTimeJobSeeker f " +
                    "LEFT JOIN f.person p " +
                    "WHERE (f.isDeleted = false OR f.isDeleted IS NULL) AND (f.isArchived = false OR f.isArchived IS NULL) " +
                    "AND (:search IS NULL OR :search = '' " +
                    "  OR LOWER(f.ftjsNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "  OR LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "  OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "  OR LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                    "AND (:status IS NULL OR f.status = :status) " +
                    "AND (:purpose IS NULL OR :purpose = '' OR f.purposeDocuments = :purpose) " +
                    "AND (:issuanceCount IS NULL OR f.issuanceCount = :issuanceCount)")
    Page<FtjsSummaryResponse> findSummaryFiltered(
            @Param("search") String search,
            @Param("status") FtjsStatus status,
            @Param("purpose") String purpose,
            @Param("issuanceCount") Integer issuanceCount,
            Pageable pageable);


    @Query(value = "SELECT new com.barangay.ftjs.dto.FtjsSummaryResponse(" +
            "f.id, " +
            "f.ftjsNumber, " +
            "CONCAT(COALESCE(p.firstName, ''), ' ', COALESCE(CONCAT(p.middleName, ' '), ''), COALESCE(p.lastName, '')), " +
            "CAST(COALESCE(f.issuanceCount, 1) AS long), " +
            "f.status, " +
            "f.purposeDocuments, " +
            "COALESCE(f.archivedAt, f.createdAt)) " +
            "FROM FirstTimeJobSeeker f " +
            "LEFT JOIN f.person p " +
            "WHERE f.isDeleted = false AND f.isArchived = true " +
            "AND (:search IS NULL OR :search = '' " +
            "  OR LOWER(f.ftjsNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "  OR LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "  OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "  OR LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR f.status = :status) " +
            "AND (:purpose IS NULL OR :purpose = '' OR f.purposeDocuments = :purpose) " +
            "AND (:issuanceCount IS NULL OR f.issuanceCount = :issuanceCount)",
            countQuery = "SELECT COUNT(f) FROM FirstTimeJobSeeker f " +
                    "LEFT JOIN f.person p " +
                    "WHERE f.isDeleted = false AND f.isArchived = true " +
                    "AND (:search IS NULL OR :search = '' " +
                    "  OR LOWER(f.ftjsNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "  OR LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "  OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "  OR LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                    "AND (:status IS NULL OR f.status = :status) " +
                    "AND (:purpose IS NULL OR :purpose = '' OR f.purposeDocuments = :purpose) " +
                    "AND (:issuanceCount IS NULL OR f.issuanceCount = :issuanceCount)")
    Page<FtjsSummaryResponse> findArchiveSummaryFiltered(
            @Param("search") String search,
            @Param("status") FtjsStatus status,
            @Param("purpose") String purpose,
            @Param("issuanceCount") Integer issuanceCount,
            Pageable pageable);


    @Query(value = "SELECT f.* FROM first_time_job_seeker f " +
            "LEFT JOIN person p ON f.person_id = p.id " +
            "WHERE f.is_deleted = true " +
            "AND (:search IS NULL OR :search = '' " +
            "  OR LOWER(f.ftjs_number) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "  OR LOWER(p.first_name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "  OR LOWER(p.last_name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "  OR LOWER(CONCAT(p.first_name, ' ', p.last_name)) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR f.status = :status) " +
            "AND (:purpose IS NULL OR :purpose = '' OR f.purpose_documents = :purpose) " +
            "AND (:issuanceCount IS NULL OR f.issuance_count = :issuanceCount)",
            countQuery = "SELECT COUNT(f.id) FROM first_time_job_seeker f " +
                    "LEFT JOIN person p ON f.person_id = p.id " +
                    "WHERE f.is_deleted = true " +
                    "AND (:search IS NULL OR :search = '' " +
                    "  OR LOWER(f.ftjs_number) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "  OR LOWER(p.first_name) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "  OR LOWER(p.last_name) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "  OR LOWER(CONCAT(p.first_name, ' ', p.last_name)) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                    "AND (:status IS NULL OR f.status = :status) " +
                    "AND (:purpose IS NULL OR :purpose = '' OR f.purpose_documents = :purpose) " +
                    "AND (:issuanceCount IS NULL OR f.issuance_count = :issuanceCount)",
            nativeQuery = true)
    Page<FirstTimeJobSeeker> findDeletedFilteredNative(
            @Param("search") String search,
            @Param("status") String status,
            @Param("purpose") String purpose,
            @Param("issuanceCount") Integer issuanceCount,
            Pageable pageable);
}
