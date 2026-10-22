package com.barangay.ftjs.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.barangay.ftjs.dto.FtjsReportTableResponse;
import com.barangay.ftjs.dto.FtjsSummaryResponse;
import com.barangay.ftjs.projection.*;
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
/**
 * Spring Data JPA Repository for managing FirstTimeJobSeeker entities and analytical queries.
 */
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

    @Query(value = "SELECT * FROM first_time_job_seeker f WHERE (:residentId IS NOT NULL AND f.resident_id = :residentId) OR (:personId IS NOT NULL AND f.person_id = :personId) ORDER BY f.created_at DESC", nativeQuery = true)
    List<FirstTimeJobSeeker> findAllHistoricalByResidentOrPerson(@Param("residentId") Long residentId, @Param("personId") Long personId);


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










    @Query(value = """
    WITH date_buckets AS (
        SELECT generate_series(
            CASE 
                WHEN :truncType = 'month' THEN DATE_TRUNC('month', CAST(:dateFrom AS timestamp))
                ELSE CAST(:dateFrom AS timestamp)
            END,
            CASE 
                WHEN :truncType = 'month' THEN DATE_TRUNC('month', CAST(:dateTo AS timestamp))
                ELSE CAST(:dateTo AS timestamp)
            END,
            CAST(:intervalStep AS interval)
        ) AS bucket_start
    ),
    filtered_records AS (
        SELECT 
            f.id,
            f.created_at
        FROM first_time_job_seeker f
        LEFT JOIN person p ON f.person_id = p.id
        WHERE f.is_deleted = false
          AND (:status IS NULL OR f.status = :status)
          AND f.created_at >= CAST(:dateFrom AS timestamp)
          AND f.created_at < (CAST(:dateTo AS timestamp) + interval '1 day')
          AND (
                :purpose IS NULL
                OR (:purpose <> 'Other' AND f.purpose_documents = :purpose)
                OR (:purpose = 'Other' AND (
                        f.purpose_documents NOT IN (
                            'First Time Jobseeker Requirement',
                            'Employment Application',
                            'Local Employment',
                            'Government Requirement'
                        ) OR f.purpose_documents IS NULL OR TRIM(f.purpose_documents) = ''
                ))
          )
          AND (
            :education IS NULL 
            OR f.educational_attainment = :education 
            OR f.educational_attainment LIKE CONCAT(:education, '%')
            OR (:education = 'Elementary' AND f.educational_attainment LIKE 'Elementary%')
            OR (:education = 'High School' AND (f.educational_attainment LIKE '%High School%' OR f.educational_attainment LIKE '%Junior High%'))
            OR (:education = 'Senior High School' AND f.educational_attainment LIKE '%Senior High%')
            OR (:education = 'Vocational / TESDA' AND (f.educational_attainment LIKE '%Vocational%' OR f.educational_attainment LIKE '%TESDA%'))
            OR (:education = 'College Graduate' AND (f.educational_attainment LIKE '%College Graduate%' OR f.educational_attainment LIKE '%Bachelor%'))
      )
          AND (:sex IS NULL OR p.gender = :sex)
    )
    SELECT 
        TO_CHAR(b.bucket_start, :dateFormat) AS label,
        b.bucket_start::date AS pointDate,
        COUNT(r.id) AS issuedCount
    FROM date_buckets b
    LEFT JOIN filtered_records r ON DATE_TRUNC(CAST(:truncType AS text), r.created_at) = b.bucket_start
    GROUP BY b.bucket_start
    ORDER BY b.bucket_start ASC
""", nativeQuery = true)
    List<TrendPointProjection> getTimelineTrendData(
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("intervalStep") String intervalStep,
            @Param("truncType") String truncType,
            @Param("dateFormat") String dateFormat,
            @Param("purpose") String purpose,
            @Param("sex") String sex,
            @Param("status") String status,
            @Param("education") String education
    );


    @Query(value = """
    SELECT 
        COALESCE(NULLIF(TRIM(f.educational_attainment), ''), 'Not Specified') AS educationalAttainment,
        COUNT(f.id) AS applicantCount
    FROM first_time_job_seeker f
    LEFT JOIN person p ON f.person_id = p.id
    WHERE f.is_deleted = false
      AND (:status IS NULL OR f.status = :status)
      AND f.created_at >= CAST(:dateFrom AS timestamp)
      AND f.created_at < (CAST(:dateTo AS timestamp) + interval '1 day')
      AND (
            :purpose IS NULL
            OR (:purpose <> 'Other' AND f.purpose_documents = :purpose)
            OR (:purpose = 'Other' AND (
                    f.purpose_documents NOT IN (
                        'First Time Jobseeker Requirement',
                        'Employment Application',
                        'Local Employment',
                        'Government Requirement'
                    ) OR f.purpose_documents IS NULL OR TRIM(f.purpose_documents) = ''
            ))
      )
      AND (:sex IS NULL OR p.gender = :sex)
      AND (
            :education IS NULL 
            OR f.educational_attainment = :education 
            OR f.educational_attainment LIKE CONCAT(:education, '%')
            OR (:education = 'Elementary' AND f.educational_attainment LIKE 'Elementary%')
            OR (:education = 'High School' AND (f.educational_attainment LIKE '%High School%' OR f.educational_attainment LIKE '%Junior High%'))
            OR (:education = 'Senior High School' AND f.educational_attainment LIKE '%Senior High%')
            OR (:education = 'Vocational / TESDA' AND (f.educational_attainment LIKE '%Vocational%' OR f.educational_attainment LIKE '%TESDA%'))
            OR (:education = 'College Graduate' AND (f.educational_attainment LIKE '%College Graduate%' OR f.educational_attainment LIKE '%Bachelor%'))
      )
    GROUP BY COALESCE(NULLIF(TRIM(f.educational_attainment), ''), 'Not Specified')
    ORDER BY applicantCount DESC
""", nativeQuery = true)
    List<EducationDistributionProjection> getEducationDistribution(
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("purpose") String purpose,
            @Param("sex") String sex,
            @Param("status") String status,
            @Param("education") String education
    );


    @Query(value = """
    SELECT 
        CASE 
            WHEN f.purpose_documents IN (
                'First Time Jobseeker Requirement',
                'Employment Application',
                'Local Employment',
                'Government Requirement'
            ) THEN f.purpose_documents
            ELSE 'Other'
        END AS purpose,
        COUNT(f.id) AS applicantCount
    FROM first_time_job_seeker f
    LEFT JOIN person p ON f.person_id = p.id
    WHERE f.is_deleted = false
      AND (:status IS NULL OR f.status = :status)
      AND f.created_at >= CAST(:dateFrom AS timestamp)
      AND f.created_at < (CAST(:dateTo AS timestamp) + interval '1 day')
      AND (
            :purpose IS NULL
            OR (:purpose <> 'Other' AND f.purpose_documents = :purpose)
            OR (:purpose = 'Other' AND (
                    f.purpose_documents NOT IN (
                        'First Time Jobseeker Requirement',
                        'Employment Application',
                        'Local Employment',
                        'Government Requirement'
                    ) OR f.purpose_documents IS NULL OR TRIM(f.purpose_documents) = ''
            ))
      )
      AND (:sex IS NULL OR p.gender = :sex)
      AND (
            :education IS NULL 
            OR f.educational_attainment = :education 
            OR f.educational_attainment LIKE CONCAT(:education, '%')
            OR (:education = 'Elementary' AND f.educational_attainment LIKE 'Elementary%')
            OR (:education = 'High School' AND (f.educational_attainment LIKE '%High School%' OR f.educational_attainment LIKE '%Junior High%'))
            OR (:education = 'Senior High School' AND f.educational_attainment LIKE '%Senior High%')
            OR (:education = 'Vocational / TESDA' AND (f.educational_attainment LIKE '%Vocational%' OR f.educational_attainment LIKE '%TESDA%'))
            OR (:education = 'College Graduate' AND (f.educational_attainment LIKE '%College Graduate%' OR f.educational_attainment LIKE '%Bachelor%'))
      )
    GROUP BY 
        CASE 
            WHEN f.purpose_documents IN (
                'First Time Jobseeker Requirement',
                'Employment Application',
                'Local Employment',
                'Government Requirement'
            ) THEN f.purpose_documents
            ELSE 'Other'
        END
    ORDER BY applicantCount DESC
""", nativeQuery = true)
    List<PurposeDistributionProjection> getPurposeDistribution(
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("purpose") String purpose,
            @Param("sex") String sex,
            @Param("status") String status,
            @Param("education") String education
    );


    @Query(value = """
    SELECT 
        CASE 
            WHEN COALESCE(f.issuance_count, 1) <= 1 THEN 'Original issuance'
            ELSE 'Re-issuance'
        END AS issuanceType,
        COUNT(f.id) AS applicantCount
    FROM first_time_job_seeker f
    LEFT JOIN person p ON f.person_id = p.id
    WHERE f.is_deleted = false
      AND (:status IS NULL OR f.status = :status)
      AND f.created_at >= CAST(:dateFrom AS timestamp)
      AND f.created_at < (CAST(:dateTo AS timestamp) + interval '1 day')
      AND (
            :purpose IS NULL
            OR (:purpose <> 'Other' AND f.purpose_documents = :purpose)
            OR (:purpose = 'Other' AND (
                    f.purpose_documents NOT IN (
                        'First Time Jobseeker Requirement',
                        'Employment Application',
                        'Local Employment',
                        'Government Requirement'
                    ) OR f.purpose_documents IS NULL OR TRIM(f.purpose_documents) = ''
            ))
      )
      AND (:sex IS NULL OR p.gender = :sex)
      AND (
            :education IS NULL 
            OR f.educational_attainment = :education 
            OR f.educational_attainment LIKE CONCAT(:education, '%')
            OR (:education = 'Elementary' AND f.educational_attainment LIKE 'Elementary%')
            OR (:education = 'High School' AND (f.educational_attainment LIKE '%High School%' OR f.educational_attainment LIKE '%Junior High%'))
            OR (:education = 'Senior High School' AND f.educational_attainment LIKE '%Senior High%')
            OR (:education = 'Vocational / TESDA' AND (f.educational_attainment LIKE '%Vocational%' OR f.educational_attainment LIKE '%TESDA%'))
            OR (:education = 'College Graduate' AND (f.educational_attainment LIKE '%College Graduate%' OR f.educational_attainment LIKE '%Bachelor%'))
      )
    GROUP BY 
        CASE 
            WHEN COALESCE(f.issuance_count, 1) <= 1 THEN 'Original issuance'
            ELSE 'Re-issuance'
        END
    ORDER BY applicantCount DESC
""", nativeQuery = true)
    List<IssuanceTypeProjection> getIssuanceTypeDistribution(
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("purpose") String purpose,
            @Param("sex") String sex,
            @Param("status") String status,
            @Param("education") String education
    );


    @Query(value = """
    SELECT 
        f.id AS id,
        f.ftjs_number AS applicationNo,
        TRIM(CONCAT(COALESCE(p.first_name, ''), ' ', COALESCE(CONCAT(p.middle_name, ' '), ''), COALESCE(p.last_name, ''), ' ', COALESCE(p.suffix, ''))) AS applicantName,
        COALESCE(f.issuance_count, 1) AS issuanceCount,
        f.purpose_documents AS purpose,
        f.status AS status,
        f.created_at AS dateSubmitted,
        p.gender AS gender,
        COALESCE(p.age, CAST(EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) AS integer)) AS age,
        f.educational_attainment AS educationalAttainment,
        f.school_institution AS institution,
        COALESCE(NULLIF(TRIM(CONCAT(COALESCE(up.first_name, ''), ' ', COALESCE(up.last_name, ''))), ''), u.username) AS releasedBy
    FROM first_time_job_seeker f
    LEFT JOIN resident r ON f.resident_id = r.resident_id
    LEFT JOIN person p ON COALESCE(f.person_id, r.person_id) = p.id
    LEFT JOIN users u ON f.verified_by = u.id
    LEFT JOIN person up ON u.person_id = up.id
    WHERE f.is_deleted = false
      AND (:status IS NULL OR f.status = :status)
      AND f.created_at >= CAST(:dateFrom AS timestamp)
      AND f.created_at < (CAST(:dateTo AS timestamp) + interval '1 day')
      AND (
            :purpose IS NULL
            OR (:purpose <> 'Other' AND f.purpose_documents = :purpose)
            OR (:purpose = 'Other' AND (
                    f.purpose_documents NOT IN (
                        'First Time Jobseeker Requirement',
                        'Employment Application',
                        'Local Employment',
                        'Government Requirement'
                    ) OR f.purpose_documents IS NULL OR TRIM(f.purpose_documents) = ''
            ))
      )
      AND (:sex IS NULL OR p.gender = :sex)
      AND (
            :education IS NULL 
            OR f.educational_attainment = :education 
            OR f.educational_attainment LIKE CONCAT(:education, '%')
            OR (:education = 'Elementary' AND f.educational_attainment LIKE 'Elementary%')
            OR (:education = 'High School' AND (f.educational_attainment LIKE '%High School%' OR f.educational_attainment LIKE '%Junior High%'))
            OR (:education = 'Senior High School' AND f.educational_attainment LIKE '%Senior High%')
            OR (:education = 'Vocational / TESDA' AND (f.educational_attainment LIKE '%Vocational%' OR f.educational_attainment LIKE '%TESDA%'))
            OR (:education = 'College Graduate' AND (f.educational_attainment LIKE '%College Graduate%' OR f.educational_attainment LIKE '%Bachelor%'))
      )
      AND (:search IS NULL OR :search = ''
            OR LOWER(f.ftjs_number) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(p.first_name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(p.last_name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(CONCAT(p.first_name, ' ', p.last_name)) LIKE LOWER(CONCAT('%', :search, '%')))
    ORDER BY f.id DESC
""", 
countQuery = """
    SELECT COUNT(f.id)
    FROM first_time_job_seeker f
    LEFT JOIN resident r ON f.resident_id = r.resident_id
    LEFT JOIN person p ON COALESCE(f.person_id, r.person_id) = p.id
    WHERE f.is_deleted = false
      AND (:status IS NULL OR f.status = :status)
      AND f.created_at >= CAST(:dateFrom AS timestamp)
      AND f.created_at < (CAST(:dateTo AS timestamp) + interval '1 day')
      AND (
            :purpose IS NULL
            OR (:purpose <> 'Other' AND f.purpose_documents = :purpose)
            OR (:purpose = 'Other' AND (
                    f.purpose_documents NOT IN (
                        'First Time Jobseeker Requirement',
                        'Employment Application',
                        'Local Employment',
                        'Government Requirement'
                    ) OR f.purpose_documents IS NULL OR TRIM(f.purpose_documents) = ''
            ))
      )
      AND (:sex IS NULL OR p.gender = :sex)
      AND (
            :education IS NULL 
            OR f.educational_attainment = :education 
            OR f.educational_attainment LIKE CONCAT(:education, '%')
            OR (:education = 'Elementary' AND f.educational_attainment LIKE 'Elementary%')
            OR (:education = 'High School' AND (f.educational_attainment LIKE '%High School%' OR f.educational_attainment LIKE '%Junior High%'))
            OR (:education = 'Senior High School' AND f.educational_attainment LIKE '%Senior High%')
            OR (:education = 'Vocational / TESDA' AND (f.educational_attainment LIKE '%Vocational%' OR f.educational_attainment LIKE '%TESDA%'))
            OR (:education = 'College Graduate' AND (f.educational_attainment LIKE '%College Graduate%' OR f.educational_attainment LIKE '%Bachelor%'))
      )
      AND (:search IS NULL OR :search = ''
            OR LOWER(f.ftjs_number) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(p.first_name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(p.last_name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(CONCAT(p.first_name, ' ', p.last_name)) LIKE LOWER(CONCAT('%', :search, '%')))
""", nativeQuery = true)
    Page<FtjsReportTableProjection> findReportTableFiltered(
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("status") String status,
            @Param("purpose") String purpose,
            @Param("sex") String sex,
            @Param("education") String education,
            @Param("search") String search,
            Pageable pageable);


    @Query(value = """
    SELECT 
        CASE 
            WHEN COALESCE(p.age, EXTRACT(YEAR FROM AGE(p.birth_date))) < 18 THEN 'Under 18'
            WHEN COALESCE(p.age, EXTRACT(YEAR FROM AGE(p.birth_date))) BETWEEN 18 AND 24 THEN '18-24'
            WHEN COALESCE(p.age, EXTRACT(YEAR FROM AGE(p.birth_date))) BETWEEN 25 AND 30 THEN '25-30'
            WHEN COALESCE(p.age, EXTRACT(YEAR FROM AGE(p.birth_date))) BETWEEN 31 AND 40 THEN '31-40'
            WHEN COALESCE(p.age, EXTRACT(YEAR FROM AGE(p.birth_date))) > 40 THEN 'Over 40'
            ELSE 'Unknown'
        END AS ageRange,
        COUNT(f.id) AS applicantCount
    FROM first_time_job_seeker f
    LEFT JOIN person p ON f.person_id = p.id
    WHERE f.is_deleted = false
      AND (:status IS NULL OR f.status = :status)
      AND f.created_at >= CAST(:dateFrom AS timestamp)
      AND f.created_at < (CAST(:dateTo AS timestamp) + interval '1 day')
      AND (
            :purpose IS NULL
            OR (:purpose <> 'Other' AND f.purpose_documents = :purpose)
            OR (:purpose = 'Other' AND (
                    f.purpose_documents NOT IN (
                        'First Time Jobseeker Requirement',
                        'Employment Application',
                        'Local Employment',
                        'Government Requirement'
                    ) OR f.purpose_documents IS NULL OR TRIM(f.purpose_documents) = ''
            ))
      )
      AND (:sex IS NULL OR p.gender = :sex)
      AND (
            :education IS NULL 
            OR f.educational_attainment = :education 
            OR f.educational_attainment LIKE CONCAT(:education, '%')
            OR (:education = 'Elementary' AND f.educational_attainment LIKE 'Elementary%')
            OR (:education = 'High School' AND (f.educational_attainment LIKE '%High School%' OR f.educational_attainment LIKE '%Junior High%'))
            OR (:education = 'Senior High School' AND f.educational_attainment LIKE '%Senior High%')
            OR (:education = 'Vocational / TESDA' AND (f.educational_attainment LIKE '%Vocational%' OR f.educational_attainment LIKE '%TESDA%'))
            OR (:education = 'College Graduate' AND (f.educational_attainment LIKE '%College Graduate%' OR f.educational_attainment LIKE '%Bachelor%'))
      )
    GROUP BY 
        CASE 
            WHEN COALESCE(p.age, EXTRACT(YEAR FROM AGE(p.birth_date))) < 18 THEN 'Under 18'
            WHEN COALESCE(p.age, EXTRACT(YEAR FROM AGE(p.birth_date))) BETWEEN 18 AND 24 THEN '18-24'
            WHEN COALESCE(p.age, EXTRACT(YEAR FROM AGE(p.birth_date))) BETWEEN 25 AND 30 THEN '25-30'
            WHEN COALESCE(p.age, EXTRACT(YEAR FROM AGE(p.birth_date))) BETWEEN 31 AND 40 THEN '31-40'
            WHEN COALESCE(p.age, EXTRACT(YEAR FROM AGE(p.birth_date))) > 40 THEN 'Over 40'
            ELSE 'Unknown'
        END
    ORDER BY applicantCount DESC
""", nativeQuery = true)
    List<AgeRangeDistributionProjection> getAgeRangeDistribution(
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("purpose") String purpose,
            @Param("sex") String sex,
            @Param("status") String status,
            @Param("education") String education
    );




    @Query(value = """
    SELECT 
        COUNT(f.id) FILTER (
            WHERE f.created_at >= DATE_TRUNC('day', CURRENT_TIMESTAMP)
        ) AS issuedToday,
        
        COUNT(f.id) FILTER (
            WHERE f.created_at >= DATE_TRUNC('week', CURRENT_TIMESTAMP)
        ) AS issuedThisWeek,
        
        COUNT(f.id) FILTER (
            WHERE UPPER(p.gender) = 'FEMALE'
        ) AS femaleCount,
        
        COUNT(f.id) FILTER (
            WHERE (
                p.birth_date IS NOT NULL 
                AND EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) BETWEEN 18 AND 24
            ) OR (
                p.birth_date IS NULL 
                AND p.age BETWEEN 18 AND 24
            )
        ) AS youthCount,
        
        COUNT(f.id) AS totalCount

    FROM first_time_job_seeker f
    LEFT JOIN person p ON f.person_id = p.id
    WHERE f.is_deleted = false
      AND f.status IN ('ISSUED', 'RE_ISSUANCE')
""", nativeQuery = true)
    FtjsDashboardStatsProjection getDashboardSummaryMetrics();
}
