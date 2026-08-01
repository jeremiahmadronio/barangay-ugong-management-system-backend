package com.barangay.cases.sharedmodel.repository;

import com.barangay.cases.sharedmodel.Case;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CaseRepositoryCustom {
    /**
     * Executes optimized database-level dynamic search, filtering, and pagination for Blotter Cases.
     */
    Page<Case> findFilteredCases(String search, String status, String natureOfComplaint, String filingType, Pageable pageable);

    /**
     * Returns un-paged list of filtered cases for exports and backward compatibility.
     */
    List<Case> findFilteredCasesList(String search, String status, String natureOfComplaint, String filingType);
}
