package com.barangay.zshared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentWeeklyOverviewDTO {
    private long blotter;
    private long lupon;
    private long bcpc;
    private long vawc;
    private long clearance;
    private long ftjs;
    private long totalWeeklyOperations;
}
