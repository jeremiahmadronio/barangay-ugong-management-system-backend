package com.barangay.kapitana.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KapitanaDemographicsResponse {
    private long totalResidents;
    private long headsOfFamily;
    private long seniorCitizens;
    private long minorsCount;
    private long adultsCount;
    private long maleCount;
    private long femaleCount;
}

