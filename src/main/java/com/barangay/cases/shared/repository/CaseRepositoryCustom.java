package com.barangay.cases.shared.repository;

import com.barangay.cases.bcpc.dto.BcpcFilterRequest;
import com.barangay.cases.blotter.dto.BlotterArchiveFilterRequest;
import com.barangay.cases.blotter.dto.BlotterArchiveItemResponse;
import com.barangay.cases.blotter.dto.BlotterFilterRequest;
import com.barangay.cases.blotter.dto.BlotterRecentDeleteItemResponse;
import com.barangay.cases.lupon.dto.LuponFilterRequest;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.vawc.dto.VawcFilterRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Custom repository interface for dynamic criteria filtering and pagination across modules.
 */
public interface CaseRepositoryCustom {

    /**
     * Retrieves paginated and filtered active Blotter cases based on multi-parameter search criteria.
     */
    Page<Case> findBlotterCasesFiltered(BlotterFilterRequest filter, Pageable pageable);

    /**
     * Retrieves paginated and filtered cases referred to Lupon under conciliation.
     */
    Page<Case> findLuponCasesFiltered(LuponFilterRequest filter, Pageable pageable);

    /**
     * Retrieves paginated and filtered VAWC cases based on risk level, status, or search keywords.
     */
    Page<Case> findVawcCasesFiltered(VawcFilterRequest filter, Pageable pageable);

    /**
     * Retrieves paginated and filtered BCPC cases categorized as CAR or CICL.
     */
    Page<Case> findBcpcCasesFiltered(BcpcFilterRequest filter, Pageable pageable);

    /**
     * Retrieves paginated archived Blotter cases for the Blotter archive table.
     */
    Page<BlotterArchiveItemResponse> findArchivedBlotterCases(BlotterArchiveFilterRequest filter, Pageable pageable);

    /**
     * Retrieves paginated soft-deleted Blotter cases for the Blotter trash table.
     */
    Page<BlotterRecentDeleteItemResponse> findRecentDeletedBlotterCases(BlotterArchiveFilterRequest filter, Pageable pageable);

    /**
     * Retrieves paginated archived Lupon cases for the Lupon archive table.
     */
    Page<BlotterArchiveItemResponse> findArchivedLuponCases(BlotterArchiveFilterRequest filter, Pageable pageable);

    /**
     * Retrieves paginated soft-deleted Lupon cases for the Lupon trash table.
     */
    Page<BlotterRecentDeleteItemResponse> findRecentDeletedLuponCases(BlotterArchiveFilterRequest filter, Pageable pageable);

    /**
     * Retrieves paginated archived VAWC cases for the VAWC archive table.
     */
    Page<BlotterArchiveItemResponse> findArchivedVawcCases(BlotterArchiveFilterRequest filter, Pageable pageable);

    /**
     * Retrieves paginated soft-deleted VAWC cases for the VAWC trash table.
     */
    Page<BlotterRecentDeleteItemResponse> findRecentDeletedVawcCases(BlotterArchiveFilterRequest filter, Pageable pageable);

    /**
     * Retrieves paginated archived BCPC cases for the BCPC archive table.
     */
    Page<BlotterArchiveItemResponse> findArchivedBcpcCases(BlotterArchiveFilterRequest filter, Pageable pageable);

    /**
     * Retrieves paginated soft-deleted BCPC cases for the BCPC trash table.
     */
    Page<BlotterRecentDeleteItemResponse> findRecentDeletedBcpcCases(BlotterArchiveFilterRequest filter, Pageable pageable);
}
