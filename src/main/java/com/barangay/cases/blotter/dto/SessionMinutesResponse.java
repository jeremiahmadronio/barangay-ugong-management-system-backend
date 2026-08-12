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
public class SessionMinutesResponse {

    private Long id;
    private Long sessionId;
    private Boolean complainantPresent;
    private Boolean respondentPresent;
    private String hearingNotes;
    private String outcome;
    private String settlementTerms;
    private LocalDateTime createdAt;
}

