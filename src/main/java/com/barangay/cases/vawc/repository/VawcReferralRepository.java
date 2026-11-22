package com.barangay.cases.vawc.repository;

import com.barangay.cases.vawc.model.VawcReferral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing external agency referrals for VAWC victims and cases.
 */
@Repository
public interface VawcReferralRepository extends JpaRepository<VawcReferral, Long> {
    List<VawcReferral> findByCasesId(Long caseId);
    List<VawcReferral> findByCasesIdOrderByIdDesc(Long caseId);


    @Query("""
        SELECT r FROM VawcReferral r
        WHERE r.createdAt BETWEEN :start AND :end
        ORDER BY r.createdAt DESC
    """)
    List<VawcReferral> findReferralsBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT COUNT(r) FROM VawcReferral r WHERE r.createdAt BETWEEN :start AND :end")
    long countReferralsBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
