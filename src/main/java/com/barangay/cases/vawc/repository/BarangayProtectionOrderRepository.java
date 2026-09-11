package com.barangay.cases.vawc.repository;

import com.barangay.cases.vawc.model.BarangayProtectionOrder;
import com.barangay.zshared.enumerated.BpoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface BarangayProtectionOrderRepository extends JpaRepository<BarangayProtectionOrder, Long> {
    Optional<BarangayProtectionOrder> findByCasesId(Long caseId);
    Optional<BarangayProtectionOrder> findByBpoControlNumber(String bpoControlNumber);
    boolean existsByBpoControlNumber(String bpoControlNumber);

    @Query("""
        SELECT COUNT(b) FROM BarangayProtectionOrder b
        LEFT JOIN b.cases c
        WHERE b.status IN :statuses
          AND (b.isDeleted IS NULL OR b.isDeleted = false)
          AND (b.isPermanentDeleted IS NULL OR b.isPermanentDeleted = false)
          AND (c IS NULL OR (
                (c.isArchived IS NULL OR c.isArchived = false)
            AND (c.isDeleted IS NULL OR c.isDeleted = false)
            AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          ))
    """)
    long countByStatusIn(@Param("statuses") Collection<BpoStatus> statuses);

    @Query("""
        SELECT b FROM BarangayProtectionOrder b
        LEFT JOIN FETCH b.cases c
        WHERE b.status IN :statuses
          AND (b.isDeleted IS NULL OR b.isDeleted = false)
          AND (b.isPermanentDeleted IS NULL OR b.isPermanentDeleted = false)
    """)
    java.util.List<BarangayProtectionOrder> findActiveBposForExpirationCheck(@Param("statuses") Collection<BpoStatus> statuses);

    @Query("""
        SELECT COUNT(b) FROM BarangayProtectionOrder b
        LEFT JOIN b.cases c
        WHERE b.status IN :statuses
          AND (
            (b.activatedAt IS NOT NULL AND b.activatedAt BETWEEN :start AND :end)
            OR (b.activatedAt IS NULL AND b.createdAt BETWEEN :start AND :end)
          )
          AND (b.isDeleted IS NULL OR b.isDeleted = false)
          AND (b.isPermanentDeleted IS NULL OR b.isPermanentDeleted = false)
          AND (c IS NULL OR (
                (c.isArchived IS NULL OR c.isArchived = false)
            AND (c.isDeleted IS NULL OR c.isDeleted = false)
            AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          ))
    """)
    long countActiveBpoBetween(
            @Param("statuses") Collection<BpoStatus> statuses,
            @Param("start") java.time.LocalDateTime start,
            @Param("end") java.time.LocalDateTime end
    );
}
