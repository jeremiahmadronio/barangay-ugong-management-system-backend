package com.barangay.cases.bcpc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BcpcReferralCreateRequest {
    @NotBlank(message = "Referred agency is required")
    private String referredTo;

    private String referredToOther;

    @NotBlank(message = "Purpose is required")
    private String purpose;

    private String purposeOther;

    private String grounds;

    private String referredBy;

    private LocalDateTime referralDate;
}

