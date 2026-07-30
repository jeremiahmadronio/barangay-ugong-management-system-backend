package com.barangay.people.officer.repository;

import com.barangay.people.officer.model.Officer;
import com.barangay.zshared.enumerated.OfficerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface OfficerRepository extends JpaRepository<Officer, Long> {

    long countByStatus(OfficerStatus status);

    List<Officer> findAllByStatus(OfficerStatus status);

    long countByStatusIn(Collection<OfficerStatus> statuses);

    long countByCreatedAtBeforeAndStatusIn(LocalDateTime timestamp, Collection<OfficerStatus> statuses);

    @Query("SELECT o FROM Officer o LEFT JOIN o.person p LEFT JOIN o.department d WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           " LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(o.position) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:deptName IS NULL OR :deptName = '' OR :deptName = 'ALL' OR LOWER(d.name) = LOWER(:deptName)) AND " +
           "((:status IS NULL AND o.status IN (com.barangay.zshared.enumerated.OfficerStatus.ACTIVE, com.barangay.zshared.enumerated.OfficerStatus.INACTIVE)) OR o.status = :status) " +
           "ORDER BY o.createdAt DESC")
    List<Officer> findFilteredOfficers(
            @Param("search") String search,
            @Param("deptName") String deptName,
            @Param("status") OfficerStatus status
    );
}
