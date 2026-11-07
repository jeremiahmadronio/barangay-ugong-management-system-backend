package com.barangay.cases.bcpc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BcpcReferralFeedbackRequest {
    // Simple response
    private String feedbackResponse; // Accepted, Declined, Status Update
    private String feedbackNotes;
    private String feedbackOfficer;
    private LocalDateTime feedbackDate;

    // Rich Feedback from Reference Architecture
    private String dateReceived;
    private String details;
    private Boolean isResolved;
    private String resolutionType;
    private String proofFileData;
    private String proofFileName;
    private Boolean declinedByAgency;
    private Boolean acceptedByAgency;
    private String reasonForDecline;

    // Structured Assessment Fields
    private String feedbackTypeCode;
    private String feedbackTypeOther;
    private String assessmentFindings;
    private String assessmentType;
    private String discernmentResult;
    private LocalDate assessmentDate;
    private String socialWorkerName;

    // Recommendation
    private String recommendationType;
    private String recommendationTypeOther;
    private String recommendedSocialWorker;
    private String externalRepresentativeName;
    private String externalRepresentativeOffice;
    private String externalRepresentativePosition;
    private String externalRepresentativeContact;
    private LocalDate recommendationDate;
    private String recommendedFamilySupport;
    private String recommendedPsychosocialAction;
}
