package com.barangay.cases.blotter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionMinutesRequest {

    private Boolean complainantPresent;
    private Boolean respondentPresent;
    private String hearingNotes;
    /** Outcome: SETTLED | FAILED | POSTPONED */
    private String outcome;
    private String settlementTerms;
}

