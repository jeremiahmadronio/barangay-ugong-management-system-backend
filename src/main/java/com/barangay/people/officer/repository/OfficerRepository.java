package com.barangay.people.officer.repository;

import com.barangay.people.officer.dto.LuponOfficerResponse;
import com.barangay.people.officer.model.Officer;
import com.barangay.zshared.enumerated.OfficerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Spring Data JPA repository for managing Officer entity database queries,
 * directory lookups, status counts, and Lupon assignments.
 */
@Repository
public interface OfficerRepository extends JpaRepository<Officer, Long> {

    // Common standard Spring Data JPA method signatures & convenience lookups
    long countByStatus(OfficerStatus status);

    List<Officer> findAllByStatus(OfficerStatus status);

    long countByStatusIn(Collection<OfficerStatus> statuses);

    long countByCreatedAtBeforeAndStatusIn(LocalDateTime timestamp, Collection<OfficerStatus> statuses);

    long countByCreatedAtBetweenAndStatusIn(LocalDateTime start, LocalDateTime end, Collection<OfficerStatus> statuses);

    // Single-line JPQL count and soft-delete queries
    @Query("SELECT COUNT(o) FROM Officer o WHERE (o.isDeleted = false OR o.isDeleted IS NULL) AND (o.isPermanentDeleted = false OR o.isPermanentDeleted IS NULL) AND (o.status IS NULL OR o.status != com.barangay.zshared.enumerated.OfficerStatus.ARCHIVED)")
    long countTotalOfficers();

    @Query("SELECT o FROM Officer o WHERE o.isDeleted = true AND (o.isPermanentDeleted = false OR o.isPermanentDeleted IS NULL) AND o.deletedAt <= :cutoff")
    List<Officer> findExpiredSoftDeletedOfficers(@Param("cutoff") LocalDateTime cutoff);

    // Multi-line complex JPQL queries (Fetch joins, DTO projection, and Filter logic)
    @Query("SELECT o FROM Officer o LEFT JOIN FETCH o.person p LEFT JOIN FETCH o.department d WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           " LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(o.position) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:deptName IS NULL OR :deptName = '' OR :deptName = 'ALL' OR LOWER(d.name) = LOWER(:deptName)) AND " +
           "((:status IS NULL AND o.status IN (com.barangay.zshared.enumerated.OfficerStatus.ACTIVE, com.barangay.zshared.enumerated.OfficerStatus.INACTIVE)) OR o.status = :status) AND " +
           "(o.isDeleted IS NULL OR o.isDeleted = false) AND " +
           "(o.isPermanentDeleted IS NULL OR o.isPermanentDeleted = false) " +
           "ORDER BY o.createdAt DESC")
    List<Officer> findFilteredOfficers(
            @Param("search") String search,
            @Param("deptName") String deptName,
            @Param("status") OfficerStatus status
    );

    @Query("""
           SELECT new com.barangay.people.officer.dto.LuponOfficerResponse(
                      o.id,
                      p.firstName,
                      p.lastName,
                      p.middleName,
                      o.position)
           FROM Officer o
           JOIN o.person p
           LEFT JOIN o.department d
           WHERE o.status = com.barangay.zshared.enumerated.OfficerStatus.ACTIVE
             AND (UPPER(d.name) = 'LUPON' )
             AND (o.isDeleted IS NULL OR o.isDeleted = false)
             AND (o.isPermanentDeleted IS NULL OR o.isPermanentDeleted = false)
           ORDER BY p.lastName ASC, p.firstName ASC
           """)
    List<LuponOfficerResponse> findLuponOfficer();
}
