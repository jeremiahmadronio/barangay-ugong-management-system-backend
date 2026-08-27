package com.barangay.cases.blotter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload containing the reason/remarks when archiving a case.
 */
public record ArchiveCaseRequest(
        @NotBlank(message = "Archive remarks are required")
        @Size(max = 500, message = "Remarks cannot exceed 500 characters")
        String remarks
) {}