package com.barangay.cases.bcpc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCiclCustodyLogDto {
    private Boolean wasApprehended;
    private String apprehendingOfficerName;
    private String apprehensionPlace;
    private String apprehensionDate;
    private String apprehensionTime;
    private String apprehensionReason;

    private String custodyResolution;
    private String turnoverDate;
    private String turnoverTime;
    private String turnedOverTo;
    private String turnedOverToSpecify;
    private String delayReason;

    private String releaseDate;
    private String releaseTime;
    private String releasedTo;
    private String recipientRelationship;
    private String contactNumber;
    private String temporaryPlacementReason;
    private String custodyRemarks;
}

