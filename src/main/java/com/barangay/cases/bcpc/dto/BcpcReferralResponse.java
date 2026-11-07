package com.barangay.cases.bcpc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BcpcReferralResponse {
    private Long id;
    private Long caseId;
    private String caseNumber;
    private String referralControlNumber;
    private String referredTo;
    private String referredToOther;
    private String purpose;
    private String purposeOther;
    private String grounds;
    private String status;
    private String referredBy;
    private LocalDateTime referralDate;

    // Feedback summary
    private String feedbackResponse;
    private String feedbackNotes;
    private String feedbackOfficer;
    private LocalDateTime feedbackDate;

    // Rich Feedback list
    private List<BcpcReferralFeedbackDto> feedbacks;

    // Cancellation
    private String cancellationReason;
    private String cancelledBy;
    private LocalDateTime cancelledAt;

    private LocalDateTime createdAt;
}
