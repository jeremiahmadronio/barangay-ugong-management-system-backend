package com.barangay.cases.bcpc.model;

import com.barangay.cases.shared.model.Case;
import com.barangay.zshared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Tracks inter-agency endorsements and referrals from BCPC to partner agencies
 * (e.g. LSWDO, PNP WCPD, Bahay Pag-asa, PAO, Hospital, or Family Court).
 */
@Entity
@Table(name = "bcpc_referrals")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BcpcReferral extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case cases;

    @Column(name = "referral_control_number", unique = true, length = 100)
    private String referralControlNumber;

    @Column(name = "referred_to", nullable = false, length = 150)
    private String referredTo;

    @Column(name = "referred_to_other", length = 150)
    private String referredToOther;

    @Column(name = "purpose", length = 150)
    private String purpose;

    @Column(name = "purpose_other", length = 150)
    private String purposeOther;

    @Column(columnDefinition = "TEXT", name = "grounds")
    private String grounds;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "Active"; // Active, Accepted, Declined, Cancelled

    @Column(name = "referred_by", length = 150)
    private String referredBy;

    @Column(name = "referral_date", nullable = false)
    private LocalDateTime referralDate;

    // Feedback Tracking
    @OneToMany(mappedBy = "referral", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt DESC")
    private java.util.List<BcpcReferralFeedback> feedbacks = new java.util.ArrayList<>();

    @Column(name = "feedback_response", length = 50)
    private String feedbackResponse; // Accepted, Declined, Status Update

    @Column(columnDefinition = "TEXT", name = "feedback_notes")
    private String feedbackNotes;

    @Column(name = "feedback_officer", length = 150)
    private String feedbackOfficer;

    @Column(name = "feedback_date")
    private LocalDateTime feedbackDate;

    // Cancellation Tracking
    @Column(columnDefinition = "TEXT", name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "cancelled_by", length = 150)
    private String cancelledBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
}

