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
public class BcpcReferralFeedbackDto {
    private Long id;
    private Long caseId;
    private Long referralId;
    private String details;
    private Boolean resolvesCase;
    private String resolutionType;
    private String proofFileData;
    private String proofFileName;
    private Boolean declinedByAgency;
    private Boolean acceptedByAgency;
    private String reasonForDecline;
    private String feedbackTypeCode;
    private String feedbackTypeOther;
    private String assessmentFindings;
    private String assessmentType;
    private String discernmentResult;
    private LocalDate assessmentDate;
    private String socialWorkerName;
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
    private LocalDate dateReceived;
    private LocalDateTime createdAt;
}

