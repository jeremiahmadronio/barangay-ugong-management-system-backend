package com.barangay.cases.blotter.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for updating blotter case status and saving dismissal/status remarks.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCaseStatusRequest {

    @NotBlank(message = "Status is required")
    private String status;

    private String remarks;
}
