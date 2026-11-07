package com.barangay.cases.bcpc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BcpcInterventionProgressRequest {

    private LocalDate sessionDate;

    /**
     * Optional updated status: ONGOING, COMPLETED, TERMINATED, REVISED
     */
    private String status;

    @NotBlank(message = "Progress notes or session summary is required.")
    @Size(max = 2000, message = "Progress notes cannot exceed 2000 characters.")
    private String progressNotes;

    @Size(max = 100, message = "Conducted by name cannot exceed 100 characters.")
    private String conductedBy;
}

