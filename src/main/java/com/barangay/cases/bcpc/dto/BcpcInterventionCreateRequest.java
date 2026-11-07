package com.barangay.cases.bcpc.dto;

import com.barangay.zshared.enumerated.BcpcInterventionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class BcpcInterventionCreateRequest {

    @NotNull(message = "Intervention Type is required.")
    private BcpcInterventionType interventionType;

    @NotBlank(message = "Program Title is required.")
    @Size(max = 150, message = "Program Title cannot exceed 150 characters.")
    private String programTitle;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters.")
    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    @Size(max = 150, message = "Facilitator or Agency cannot exceed 150 characters.")
    private String facilitatorOrAgency;

    @Size(max = 3000, message = "Initial progress notes cannot exceed 3000 characters.")
    private String initialNotes;
}

