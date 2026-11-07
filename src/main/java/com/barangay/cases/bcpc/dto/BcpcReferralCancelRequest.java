package com.barangay.cases.bcpc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BcpcReferralCancelRequest {
    @NotBlank(message = "Cancellation reason is required")
    private String cancellationReason;

    private String cancelledBy;
}

