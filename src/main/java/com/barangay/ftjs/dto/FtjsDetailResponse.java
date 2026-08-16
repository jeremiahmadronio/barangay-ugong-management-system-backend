package com.barangay.ftjs.dto;

import com.barangay.zshared.dto.ResidentDetailDto;
import com.barangay.zshared.enumerated.FtjsStatus;

import java.time.LocalDateTime;

/**
 * DTO detailed response containing full applicant and issuance information.
 */
public record FtjsDetailResponse(

        String ftjsApplicationNumber,
        String applicantFullName,
        String purpose,

        ResidentDetailDto detail,
        FtjsStatus status,
        Integer issuanceCount,

        String educationalAttainment,
        String course,
        String institution,

        String verifiedBy,
        LocalDateTime dateReleased,
        byte[] validId,
        byte[] oath


) {
}
