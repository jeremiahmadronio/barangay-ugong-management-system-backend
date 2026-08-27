package com.barangay.cases.shared.repository;

import com.barangay.cases.blotter.dto.BlotterArchiveFilterRequest;
import com.barangay.cases.blotter.dto.BlotterArchiveItemResponse;
import com.barangay.cases.blotter.dto.BlotterRecentDeleteItemResponse;
import com.barangay.cases.blotter.dto.BlotterFilterRequest;
import com.barangay.cases.shared.model.Case;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CaseRepositoryCustom {

    Page<Case> findBlotterCasesFiltered(BlotterFilterRequest filter, Pageable pageable);

    Page<BlotterArchiveItemResponse> findArchivedBlotterCases(BlotterArchiveFilterRequest filter, Pageable pageable);

    Page<BlotterRecentDeleteItemResponse> findRecentDeletedBlotterCases(BlotterArchiveFilterRequest filter, Pageable pageable);
}
