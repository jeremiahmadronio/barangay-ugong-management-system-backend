package com.barangay.cases.vawc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record BpoServeRequest(
        @NotBlank(message = "Served by is required")
        String servedBy,

        @NotNull(message = "Date served is required")
        LocalDateTime dateServed,

        @NotBlank(message = "Service method is required")
        String serviceMethod,

        String witness1Name,
        String witness2Name,
        String certificationFileName,
        String certificationFileData
) {}
