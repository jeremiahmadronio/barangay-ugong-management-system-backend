package com.barangay.kapitana.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KapitanaRecentCaseItem {
    private Long id;
    private String caseNumber;
    private String department;
    private String incidentNature;
    private String complainantName;
    private String respondentName;
    private String caseStatus;
    private String filingType;
    private LocalDateTime dateFiled;
}
