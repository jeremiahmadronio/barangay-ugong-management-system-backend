package com.barangay.cases.lupon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PanelAttendanceResponse {
    private Long id;
    private Long panelMemberId;
    private Long officerId;
    private String officerName;
    private String position;
    private Boolean isPresent;
}
