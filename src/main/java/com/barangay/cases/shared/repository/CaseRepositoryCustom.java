package com.barangay.cases.shared.repository;

import com.barangay.cases.blotter.dto.BlotterArchiveFilterRequest;
import com.barangay.cases.blotter.dto.BlotterArchiveItemResponse;
import com.barangay.cases.blotter.dto.BlotterRecentDeleteItemResponse;
import com.barangay.cases.blotter.dto.BlotterFilterRequest;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.lupon.dto.LuponFilterRequest;
import com.barangay.cases.bcpc.dto.BcpcFilterRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.barangay.cases.vawc.dto.VawcFilterRequest;

public interface CaseRepositoryCustom {

    Page<Case> findBlotterCasesFiltered(BlotterFilterRequest filter, Pageable pageable);

    Page<Case> findLuponCasesFiltered(LuponFilterRequest filter, Pageable pageable);

    Page<Case> findVawcCasesFiltered(VawcFilterRequest filter, Pageable pageable);

    Page<Case> findBcpcCasesFiltered(BcpcFilterRequest filter, Pageable pageable);

    Page<BlotterArchiveItemResponse> findArchivedBlotterCases(BlotterArchiveFilterRequest filter, Pageable pageable);

    Page<BlotterRecentDeleteItemResponse> findRecentDeletedBlotterCases(BlotterArchiveFilterRequest filter, Pageable pageable);

    Page<BlotterArchiveItemResponse> findArchivedLuponCases(BlotterArchiveFilterRequest filter, Pageable pageable);

    Page<BlotterRecentDeleteItemResponse> findRecentDeletedLuponCases(BlotterArchiveFilterRequest filter, Pageable pageable);

    Page<BlotterArchiveItemResponse> findArchivedVawcCases(BlotterArchiveFilterRequest filter, Pageable pageable);

    Page<BlotterRecentDeleteItemResponse> findRecentDeletedVawcCases(BlotterArchiveFilterRequest filter, Pageable pageable);
}
