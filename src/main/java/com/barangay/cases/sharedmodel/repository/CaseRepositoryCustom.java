package com.barangay.cases.sharedmodel.repository;

import com.barangay.cases.blotter.dto.BlotterFilterRequest;
import com.barangay.cases.sharedmodel.Case;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CaseRepositoryCustom {

    Page<Case> findBlotterCasesFiltered(BlotterFilterRequest filter, Pageable pageable);
}
