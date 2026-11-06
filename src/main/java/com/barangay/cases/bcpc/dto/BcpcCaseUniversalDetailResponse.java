package com.barangay.cases.bcpc.dto;

/**
 * Universal response payload for viewing a BCPC case (either CAR or CICL).
 */
public record BcpcCaseUniversalDetailResponse(
        Long id,
        String caseNumber,
        String category, // "CAR" | "CICL"
        String filingType,
        String caseStatus,
        BcpcCarCaseDetailResponse carDetails,
        BcpcCiclCaseDetailResponse ciclDetails
) {}

