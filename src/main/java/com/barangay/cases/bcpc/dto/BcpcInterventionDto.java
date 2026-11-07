package com.barangay.cases.bcpc.dto;

import com.barangay.zshared.enumerated.BcpcInterventionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BcpcInterventionDto {
    private Long id;
    private Long caseId;
    private Long bcpcCaseDetailsId;
    private BcpcInterventionType interventionType;
    private String interventionTypeDisplay;
    private String programTitle;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String facilitatorOrAgency;
    private String progressNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}

