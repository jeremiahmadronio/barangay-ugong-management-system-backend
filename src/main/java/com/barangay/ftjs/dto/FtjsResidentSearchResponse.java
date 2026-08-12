package com.barangay.ftjs.dto;

import java.util.List;

public record FtjsResidentSearchResponse(
        Long residentId,
        String fullName,
        Integer age,
        String gender,
        String civilStatus,
        String contactNumber,
        String completeAddress,
        String residencyDate,
        Boolean hasAvailedFtjs,
        String priorTrackingNumber,
        String priorDateSubmitted,

        List<FtjsIssuanceHistoryResponse> issuanceHistories,
        List<FtjsBarangayCaseHistoryResponse> caseHistoryList
) {
}
