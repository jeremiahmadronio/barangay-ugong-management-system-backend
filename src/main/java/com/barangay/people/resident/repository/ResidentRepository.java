package com.barangay.people.resident.repository;

import com.barangay.people.resident.model.Resident;
import com.barangay.zshared.enumerated.ResidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Resident entity database persistence,
 * demographic analytics, lifecycle state queries, and directory lookups.
 */
@Repository
public interface ResidentRepository extends JpaRepository<Resident, Long> {

    // Common standard Spring Data JPA method signatures & convenience lookups
    Optional<Resident> findByBarangayIdNumber(String barangayIdNumber);

    Optional<Resident> findByPersonId(Long personId);

    Optional<Resident> findByPerson_Id(Long personId);

    boolean existsByBarangayIdNumber(String barangayIdNumber);

    long countByStatus(ResidentStatus status);

    List<Resident> findAllByStatus(ResidentStatus status);

    long countByStatusIn(Collection<ResidentStatus> statuses);

    long countByStatusInAndIsHeadOfFamilyTrue(Collection<ResidentStatus> statuses);

    long countByStatusInAndIsHeadOfFamilyTrueAndCreatedAtBetween(Collection<ResidentStatus> statuses, LocalDateTime start, LocalDateTime end);

    long countByStatusInAndCreatedAtBetween(Collection<ResidentStatus> statuses, LocalDateTime start, LocalDateTime end);

    // Single-line JPQL count and lookup queries
    @Query("SELECT COUNT(r) FROM Resident r WHERE r.isDeleted = true AND (r.isPermanentDeleted = false OR r.isPermanentDeleted IS NULL)")
    long countTrashResidents();

    @Query("SELECT COUNT(r) FROM Resident r WHERE (r.isDeleted = false OR r.isDeleted IS NULL) AND (r.isPermanentDeleted = false OR r.isPermanentDeleted IS NULL) AND (r.status IS NULL OR r.status != com.barangay.zshared.enumerated.ResidentStatus.ARCHIVED)")
    long countTotalResidents();

    @Query("SELECT COUNT(r) FROM Resident r WHERE (r.isDeleted = false OR r.isDeleted IS NULL) AND (r.isPermanentDeleted = false OR r.isPermanentDeleted IS NULL) AND (r.status IS NULL OR r.status != com.barangay.zshared.enumerated.ResidentStatus.ARCHIVED) AND r.isHeadOfFamily = true")
    long countActiveHeadsOfFamily();

    @Query("SELECT COUNT(r) FROM Resident r JOIN r.person p WHERE (r.isDeleted = false OR r.isDeleted IS NULL) AND (r.isPermanentDeleted = false OR r.isPermanentDeleted IS NULL) AND (r.status IS NULL OR r.status != com.barangay.zshared.enumerated.ResidentStatus.ARCHIVED) AND UPPER(p.gender) LIKE UPPER(CONCAT(:genderPrefix, '%'))")
    long countActiveByGenderPrefix(@Param("genderPrefix") String genderPrefix);

    @Query("SELECT COUNT(r) FROM Resident r JOIN r.person p WHERE r.status IN :statuses AND (p.age >= 60 OR (p.birthDate IS NOT NULL AND p.birthDate <= :seniorCutoff))")
    long countSeniorCitizensByStatuses(@Param("seniorCutoff") LocalDate seniorCutoff, @Param("statuses") Collection<ResidentStatus> statuses);

    @Query("SELECT COUNT(r) FROM Resident r JOIN r.person p WHERE r.status IN :statuses AND (p.age >= 60 OR (p.birthDate IS NOT NULL AND p.birthDate <= :seniorCutoff)) AND r.createdAt BETWEEN :start AND :end")
    long countSeniorCitizensByStatusesBetween(@Param("seniorCutoff") LocalDate seniorCutoff, @Param("statuses") Collection<ResidentStatus> statuses, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT r.barangayIdNumber FROM Resident r WHERE r.barangayIdNumber IS NOT NULL ORDER BY r.residentId DESC LIMIT 1")
    Optional<String> findLatestBarangayIdNumber();

    @Query("SELECT r FROM Resident r ORDER BY r.residentId DESC LIMIT 1")
    Optional<Resident> findLatestResident();

    @Query("SELECT r.householdNumber FROM Resident r WHERE r.householdNumber IS NOT NULL ORDER BY r.residentId DESC LIMIT 1")
    Optional<String> findLatestHouseholdNumber();

    @Query("SELECT r FROM Resident r JOIN FETCH r.person p WHERE LOWER(p.lastName) = LOWER(:lastName) AND r.status = 'ACTIVE'")
    List<Resident> findActiveResidentsByLastName(@Param("lastName") String lastName);

    @Query("SELECT COUNT(r) FROM Resident r WHERE (r.isDeleted = false OR r.isDeleted IS NULL) AND (r.isPermanentDeleted = false OR r.isPermanentDeleted IS NULL) AND (r.status IS NULL OR r.status != com.barangay.zshared.enumerated.ResidentStatus.ARCHIVED) AND r.createdAt BETWEEN :start AND :end")
    long countResidentsCreatedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT r FROM Resident r WHERE r.isDeleted = true AND (r.isPermanentDeleted = false OR r.isPermanentDeleted IS NULL) AND r.deletedAt <= :cutoff")
    List<Resident> findExpiredSoftDeletedResidents(@Param("cutoff") LocalDateTime cutoff);

    // Multi-line complex JPQL queries (Joins, Fetch joins, Demographics, and Filter logic)
    @Query("""
        SELECT COUNT(r) FROM Resident r JOIN r.person p
        WHERE (r.isDeleted = false OR r.isDeleted IS NULL)
          AND (r.isPermanentDeleted = false OR r.isPermanentDeleted IS NULL)
          AND (r.status IS NULL OR r.status != com.barangay.zshared.enumerated.ResidentStatus.ARCHIVED)
          AND (p.age >= 60 OR (p.birthDate IS NOT NULL AND p.birthDate <= :seniorCutoff))
    """)
    long countActiveSeniorCitizens(@Param("seniorCutoff") LocalDate seniorCutoff);

    @Query("""
        SELECT COUNT(r) FROM Resident r JOIN r.person p
        WHERE (r.isDeleted = false OR r.isDeleted IS NULL)
          AND (r.isPermanentDeleted = false OR r.isPermanentDeleted IS NULL)
          AND (r.status IS NULL OR r.status != com.barangay.zshared.enumerated.ResidentStatus.ARCHIVED)
          AND (p.age < 18 OR (p.birthDate IS NOT NULL AND p.birthDate > :minorCutoff))
    """)
    long countActiveMinors(@Param("minorCutoff") LocalDate minorCutoff);

    @Query("SELECT r FROM Resident r JOIN FETCH r.person p WHERE " +
           "(r.isDeleted = false OR r.isDeleted IS NULL) AND (r.isPermanentDeleted = false OR r.isPermanentDeleted IS NULL) AND " +
           "(:searchPattern IS NULL OR LOWER(p.firstName) LIKE :searchPattern OR LOWER(p.lastName) LIKE :searchPattern OR LOWER(r.barangayIdNumber) LIKE :searchPattern) AND " +
           "((:status IS NULL AND r.status IN (com.barangay.zshared.enumerated.ResidentStatus.ACTIVE, com.barangay.zshared.enumerated.ResidentStatus.INACTIVE)) OR r.status = :status) AND " +
           "(:gender IS NULL OR p.gender = :gender)")
    List<Resident> findFilteredResidents(
            @Param("searchPattern") String searchPattern,
            @Param("status") ResidentStatus status,
            @Param("gender") String gender
    );

    @Query("SELECT r FROM Resident r LEFT JOIN FETCH r.person p WHERE (r.isDeleted = false OR r.isDeleted IS NULL) AND (r.isPermanentDeleted = false OR r.isPermanentDeleted IS NULL) AND (r.status IS NULL OR r.status != com.barangay.zshared.enumerated.ResidentStatus.ARCHIVED) AND (" +
           ":search IS NULL OR TRIM(:search) = '' OR " +
           "LOWER(p.firstName) LIKE LOWER(CONCAT(TRIM(:search), '%')) OR " +
           "LOWER(p.lastName) LIKE LOWER(CONCAT(TRIM(:search), '%')) OR " +
           "LOWER(COALESCE(p.middleName, '')) LIKE LOWER(CONCAT(TRIM(:search), '%')) OR " +
           "LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT(TRIM(:search), '%')) OR " +
           "LOWER(CONCAT(p.lastName, ' ', p.firstName)) LIKE LOWER(CONCAT(TRIM(:search), '%')) OR " +
           "LOWER(CONCAT(' ', p.firstName, ' ', COALESCE(p.middleName, ''), ' ', p.lastName)) LIKE LOWER(CONCAT('% ', TRIM(:search), '%')) OR " +
           "LOWER(r.barangayIdNumber) LIKE LOWER(CONCAT(TRIM(:search), '%'))) " +
           "ORDER BY " +
           "CASE " +
           "  WHEN LOWER(p.firstName) LIKE LOWER(CONCAT(TRIM(:search), '%')) THEN 1 " +
           "  WHEN LOWER(p.lastName) LIKE LOWER(CONCAT(TRIM(:search), '%')) THEN 2 " +
           "  ELSE 3 " +
           "END ASC, p.firstName ASC, p.lastName ASC")
    List<Resident> searchActiveResidents(@Param("search") String search);
}
