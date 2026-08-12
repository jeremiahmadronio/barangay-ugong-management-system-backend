package com.barangay.cases.shared.repository;

import com.barangay.cases.blotter.dto.BlotterFilterRequest;
import com.barangay.cases.shared.model.Case;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CaseRepositoryCustom {

    Page<Case> findBlotterCasesFiltered(BlotterFilterRequest filter, Pageable pageable);
}
