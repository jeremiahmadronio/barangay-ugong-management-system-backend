package com.barangay.cases.blotter.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for reopening a closed or dismissed blotter case.
 *
 * @param remarks reason/remarks for reopening the case
 */
public record ReopenCaseRequest(
        @NotBlank(message = "Remarks for reopening the case are required")
        String remarks
) {}
