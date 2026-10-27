package com.barangay.ftjs.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record LinkedBlotterResponseDTO(
        Long blotterId,
        String blotterNumber,
        String caseType,
        String mainCaseStatus,
        @JsonProperty("documentReissuanceStatus")
        @JsonAlias({"DocumentReissuanceStatus", "documentReissuanceStatus"})
        String documentReissuanceStatus,
        @JsonProperty("documentReissuanceRemarks")
        @JsonAlias({"ftjsReissuanceRemarks", "documentReissuanceRemarks"})
        String documentReissuanceRemarks,
        String natureOfComplaint,
        @JsonProperty("dateOfIncident")
        @JsonAlias({"dateOfLoss", "dateOfIncident"})
        LocalDate dateOfIncident,
        @JsonProperty("timeOfIncident")
        @JsonAlias({"timeOfLoss", "timeOfIncident"})
        LocalTime timeOfIncident,
        String placeOfIncident,
        String reason,
        boolean hasAffidavit,
        Long affidavitEvidenceId,
        String affidavitFileName,
        String reviewedBy,
        LocalDateTime reviewedAt,
        boolean isAlreadyReissued
) {
    @JsonProperty("DocumentReissuanceStatus")
    public String getDocumentReissuanceStatusLegacy() {
        return documentReissuanceStatus;
    }

    @JsonProperty("ftjsReissuanceRemarks")
    public String getFtjsReissuanceRemarksLegacy() {
        return documentReissuanceRemarks;
    }

    @JsonProperty("dateOfLoss")
    public LocalDate getDateOfLossLegacy() {
        return dateOfIncident;
    }

    @JsonProperty("timeOfLoss")
    public LocalTime getTimeOfLossLegacy() {
        return timeOfIncident;
    }
}
