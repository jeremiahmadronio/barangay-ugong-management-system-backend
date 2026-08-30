package com.barangay.cases.lupon.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecordConciliationMinutesRequest {

    private Boolean complainantPresent;
    private Boolean respondentPresent;
    private String hearingNotes;
    private String outcome; // SETTLED | FAILED | POSTPONED
    private String settlementTerms;

    /** Map of panelMemberId -> isPresent (e.g. { 1: true, 2: false }) */
    private Map<Long, Boolean> panelAttendance;
}
