package com.barangay.cases.bcpc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BcpcCustodyLogDto {
    private String turnoverAction; // "NEW_TURNOVER", "CORRECTION", "LSWDO_PHYSICAL_TURNOVER"
    private String editReason;
    private String custodyStatus;
    private Boolean needsMedicalAttention;
    private Boolean safeInCurrentLocation;
    private Boolean temporarySafetyPlacementRequired;
    private String temporaryPlacement; // "Parent / Guardian / Caregiver", "LSWDO", "Barangay of Residence", "Other Authorized Person", "Temporarily staying at the barangay while awaiting turnover"
    private String initialResponsibility; // JSON with receiving details
    private String finalPlacement;
    private String contactNumber;
    private String transferNotes;
    private LocalDate transferDate;
    private String transferTime;
    private String temporaryPlacementReason;
    private LocalDate dateOfDecision;
    private String justification;
    private Boolean healthCoordinationConfirmed;
    private Boolean childGuidanceProvided;
    private Boolean parentAdviceProvided;
    private String guidanceNotes;
    private List<String> correctionAreas;

    // Parsed convenience display helpers for frontend
    private String receivingPersonName;
    private String receivingRelationship;
    private String receivingOffice;
    private String immediateActionNotes;
    private Boolean carGuidanceApplicable;

    // CICL custody resolution fields
    private Boolean wasApprehended;
    private String apprehendingOfficerName;
    private String apprehensionPlace;
    private String apprehensionDate;
    private String apprehensionTime;
    private String custodyResolution;
    private String turnoverDate;
    private String turnoverTime;
    private String delayReason;
    private String custodyRemarks;
}

