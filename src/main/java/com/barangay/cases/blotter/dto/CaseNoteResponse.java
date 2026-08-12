package com.barangay.cases.blotter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseNoteResponse {
    private Long id;
    private Long caseId;
    private String note;
    private String createdBy;
    private LocalDateTime createdAt;
}
