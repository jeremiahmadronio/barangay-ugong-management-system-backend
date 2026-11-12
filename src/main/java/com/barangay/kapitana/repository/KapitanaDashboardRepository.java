package com.barangay.kapitana.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.barangay.cases.shared.model.Case;

@Repository
public interface KapitanaDashboardRepository extends JpaRepository<Case, Long> {

    // 1. BLOTTER COUNTS (Strictly non-deleted, non-permanently-deleted)
    @Query("""
        SELECT COUNT(c) FROM Case c
        WHERE (c.isDeleted IS FALSE OR c.isDeleted IS NULL)
          AND (c.isPermanentDeleted IS FALSE OR c.isPermanentDeleted IS NULL)
          AND (c.isArchived IS FALSE OR c.isArchived IS NULL)
          AND c.caseStatus NOT IN (com.barangay.zshared.enumerated.CaseStatus.SETTLED, com.barangay.zshared.enumerated.CaseStatus.DISMISSED)
          AND ((c.department IS NOT NULL AND UPPER(c.department.name) = 'BLOTTER')
               OR (UPPER(c.caseNumber) LIKE '%BLOTTER%'
                   AND UPPER(c.caseNumber) NOT LIKE '%LUPON%'
                   AND UPPER(c.caseNumber) NOT LIKE '%VAWC%'
                   AND UPPER(c.caseNumber) NOT LIKE '%BCPC%'))
    """)
    long countActiveBlotterCases();

    @Query("""
        SELECT COUNT(c) FROM Case c
        WHERE (c.isDeleted IS FALSE OR c.isDeleted IS NULL)
          AND (c.isPermanentDeleted IS FALSE OR c.isPermanentDeleted IS NULL)
          AND c.caseStatus = com.barangay.zshared.enumerated.CaseStatus.SETTLED
          AND ((c.department IS NOT NULL AND UPPER(c.department.name) = 'BLOTTER')
               OR (UPPER(c.caseNumber) LIKE '%BLOTTER%'
                   AND UPPER(c.caseNumber) NOT LIKE '%LUPON%'
                   AND UPPER(c.caseNumber) NOT LIKE '%VAWC%'
                   AND UPPER(c.caseNumber) NOT LIKE '%BCPC%'))
    """)
    long countSettledBlotterCases();

    @Query("""
        SELECT COUNT(c) FROM Case c
        WHERE (c.isDeleted IS FALSE OR c.isDeleted IS NULL)
          AND (c.isPermanentDeleted IS FALSE OR c.isPermanentDeleted IS NULL)
          AND ((c.department IS NOT NULL AND UPPER(c.department.name) = 'BLOTTER')
               OR (UPPER(c.caseNumber) LIKE '%BLOTTER%'
                   AND UPPER(c.caseNumber) NOT LIKE '%LUPON%'
                   AND UPPER(c.caseNumber) NOT LIKE '%VAWC%'
                   AND UPPER(c.caseNumber) NOT LIKE '%BCPC%'))
    """)
    long countTotalBlotterCases();

    // 2. LUPON COUNTS (Strictly non-deleted, non-permanently-deleted)
    @Query("""
        SELECT COUNT(c) FROM Case c
        WHERE (c.isDeleted IS FALSE OR c.isDeleted IS NULL)
          AND (c.isPermanentDeleted IS FALSE OR c.isPermanentDeleted IS NULL)
          AND (c.isArchived IS FALSE OR c.isArchived IS NULL)
          AND c.caseStatus NOT IN (com.barangay.zshared.enumerated.CaseStatus.SETTLED, com.barangay.zshared.enumerated.CaseStatus.DISMISSED)
          AND ((c.department IS NOT NULL AND UPPER(c.department.name) = 'LUPON')
               OR c.luponReferral IS NOT NULL
               OR c.caseStatus = com.barangay.zshared.enumerated.CaseStatus.UNDER_CONCILIATION
               OR UPPER(c.caseNumber) LIKE '%LUPON%')
    """)
    long countActiveLuponCases();

    @Query("""
        SELECT COUNT(c) FROM Case c
        WHERE (c.isDeleted IS FALSE OR c.isDeleted IS NULL)
          AND (c.isPermanentDeleted IS FALSE OR c.isPermanentDeleted IS NULL)
          AND c.caseStatus = com.barangay.zshared.enumerated.CaseStatus.SETTLED
          AND ((c.department IS NOT NULL AND UPPER(c.department.name) = 'LUPON')
               OR c.luponReferral IS NOT NULL
               OR c.caseStatus = com.barangay.zshared.enumerated.CaseStatus.UNDER_CONCILIATION
               OR UPPER(c.caseNumber) LIKE '%LUPON%')
    """)
    long countSettledLuponCases();

    @Query("""
        SELECT COUNT(c) FROM Case c
        WHERE (c.isDeleted IS FALSE OR c.isDeleted IS NULL)
          AND (c.isPermanentDeleted IS FALSE OR c.isPermanentDeleted IS NULL)
          AND ((c.department IS NOT NULL AND UPPER(c.department.name) = 'LUPON')
               OR c.luponReferral IS NOT NULL
               OR c.caseStatus = com.barangay.zshared.enumerated.CaseStatus.UNDER_CONCILIATION
               OR UPPER(c.caseNumber) LIKE '%LUPON%')
    """)
    long countTotalLuponCases();

    // 3. VAWC COUNTS (Strictly non-deleted, non-permanently-deleted)
    @Query("""
        SELECT COUNT(c) FROM Case c
        WHERE (c.isDeleted IS FALSE OR c.isDeleted IS NULL)
          AND (c.isPermanentDeleted IS FALSE OR c.isPermanentDeleted IS NULL)
          AND (c.isArchived IS FALSE OR c.isArchived IS NULL)
          AND c.caseStatus NOT IN (com.barangay.zshared.enumerated.CaseStatus.SETTLED, com.barangay.zshared.enumerated.CaseStatus.DISMISSED)
          AND ((c.department IS NOT NULL AND UPPER(c.department.name) = 'VAWC')
               OR UPPER(c.caseNumber) LIKE '%VAWC%')
    """)
    long countActiveVawcCases();

    @Query("""
        SELECT COUNT(c) FROM Case c
        WHERE (c.isDeleted IS FALSE OR c.isDeleted IS NULL)
          AND (c.isPermanentDeleted IS FALSE OR c.isPermanentDeleted IS NULL)
          AND c.caseStatus = com.barangay.zshared.enumerated.CaseStatus.SETTLED
          AND ((c.department IS NOT NULL AND UPPER(c.department.name) = 'VAWC')
               OR UPPER(c.caseNumber) LIKE '%VAWC%')
    """)
    long countSettledVawcCases();

    @Query("""
        SELECT COUNT(c) FROM Case c
        WHERE (c.isDeleted IS FALSE OR c.isDeleted IS NULL)
          AND (c.isPermanentDeleted IS FALSE OR c.isPermanentDeleted IS NULL)
          AND ((c.department IS NOT NULL AND UPPER(c.department.name) = 'VAWC')
               OR UPPER(c.caseNumber) LIKE '%VAWC%')
    """)
    long countTotalVawcCases();

    // 4. BCPC COUNTS (Strictly non-deleted, non-permanently-deleted)
    @Query("""
        SELECT COUNT(c) FROM Case c
        WHERE (c.isDeleted IS FALSE OR c.isDeleted IS NULL)
          AND (c.isPermanentDeleted IS FALSE OR c.isPermanentDeleted IS NULL)
          AND (c.isArchived IS FALSE OR c.isArchived IS NULL)
          AND c.caseStatus NOT IN (com.barangay.zshared.enumerated.CaseStatus.SETTLED, com.barangay.zshared.enumerated.CaseStatus.DISMISSED)
          AND ((c.department IS NOT NULL AND UPPER(c.department.name) = 'BCPC')
               OR UPPER(c.caseNumber) LIKE '%BCPC%')
    """)
    long countActiveBcpcCases();

    @Query("""
        SELECT COUNT(c) FROM Case c
        WHERE (c.isDeleted IS FALSE OR c.isDeleted IS NULL)
          AND (c.isPermanentDeleted IS FALSE OR c.isPermanentDeleted IS NULL)
          AND c.caseStatus = com.barangay.zshared.enumerated.CaseStatus.SETTLED
          AND ((c.department IS NOT NULL AND UPPER(c.department.name) = 'BCPC')
               OR UPPER(c.caseNumber) LIKE '%BCPC%')
    """)
    long countSettledBcpcCases();

    @Query("""
        SELECT COUNT(c) FROM Case c
        WHERE (c.isDeleted IS FALSE OR c.isDeleted IS NULL)
          AND (c.isPermanentDeleted IS FALSE OR c.isPermanentDeleted IS NULL)
          AND ((c.department IS NOT NULL AND UPPER(c.department.name) = 'BCPC')
               OR UPPER(c.caseNumber) LIKE '%BCPC%')
    """)
    long countTotalBcpcCases();

    // 5. OVERALL ACTIVE AND SETTLED
    @Query("""
        SELECT COUNT(c) FROM Case c
        WHERE (c.isDeleted IS FALSE OR c.isDeleted IS NULL)
          AND (c.isPermanentDeleted IS FALSE OR c.isPermanentDeleted IS NULL)
          AND (c.isArchived IS FALSE OR c.isArchived IS NULL)
          AND c.caseStatus NOT IN (com.barangay.zshared.enumerated.CaseStatus.SETTLED, com.barangay.zshared.enumerated.CaseStatus.DISMISSED)
    """)
    long countTotalActiveCases();

    @Query("""
        SELECT COUNT(c) FROM Case c
        WHERE (c.isDeleted IS FALSE OR c.isDeleted IS NULL)
          AND (c.isPermanentDeleted IS FALSE OR c.isPermanentDeleted IS NULL)
          AND c.caseStatus = com.barangay.zshared.enumerated.CaseStatus.SETTLED
    """)
    long countTotalSettledCases();

    @Query("""
        SELECT COUNT(c) FROM Case c
        WHERE (c.isDeleted IS FALSE OR c.isDeleted IS NULL)
          AND (c.isPermanentDeleted IS FALSE OR c.isPermanentDeleted IS NULL)
    """)
    long countTotalNonDeletedCases();

    // 6. RECENT CASES (Unified Cross-Department Feed)
    @Query("""
        SELECT c FROM Case c
        LEFT JOIN FETCH c.incidentDetail id
        LEFT JOIN FETCH c.complainant comp
        LEFT JOIN FETCH comp.person compPerson
        LEFT JOIN FETCH c.respondent resp
        LEFT JOIN FETCH resp.person respPerson
        LEFT JOIN FETCH c.department d
        WHERE (c.isDeleted IS FALSE OR c.isDeleted IS NULL)
          AND (c.isPermanentDeleted IS FALSE OR c.isPermanentDeleted IS NULL)
        ORDER BY c.createdAt DESC
    """)
    List<Case> findRecentCasesAcrossDepartments(Pageable pageable);
}
