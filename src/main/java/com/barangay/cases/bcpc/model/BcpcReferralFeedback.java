package com.barangay.cases.bcpc.model;

import com.barangay.cases.shared.model.Case;
import com.barangay.zshared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Represents official feedback received from receiving partner agencies regarding a BCPC referral.
 */
@Entity
@Table(
    name = "bcpc_referral_feedbacks",
    indexes = {
        @Index(name = "idx_bcpc_referral_feedbacks_case_id", columnList = "case_id"),
        @Index(name = "idx_bcpc_referral_feedbacks_referral_id", columnList = "referral_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BcpcReferralFeedback extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case cases;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referral_id", nullable = false)
    private BcpcReferral referral;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "resolves_case", nullable = false)
    private Boolean resolvesCase = false;

    @Column(name = "resolution_type", length = 100)
    private String resolutionType;

    @Column(name = "proof_file_data", columnDefinition = "TEXT")
    private String proofFileData;

    @Column(name = "proof_file_name")
    private String proofFileName;

    @Column(name = "declined_by_agency", nullable = false)
    private Boolean declinedByAgency = false;

    @Column(name = "accepted_by_agency", nullable = false)
    private Boolean acceptedByAgency = false;

    @Column(name = "reason_for_decline", columnDefinition = "TEXT")
    private String reasonForDecline;

    @Column(name = "feedback_type_code", length = 50)
    private String feedbackTypeCode;

    @Column(name = "feedback_type_other", length = 100)
    private String feedbackTypeOther;

    @Column(columnDefinition = "TEXT", name = "assessment_findings")
    private String assessmentFindings;

    @Column(name = "assessment_type", length = 100)
    private String assessmentType;

    @Column(name = "discernment_result", length = 50)
    private String discernmentResult;

    @Column(name = "assessment_date")
    private LocalDate assessmentDate;

    @Column(name = "social_worker_name", length = 150)
    private String socialWorkerName;

    @Column(name = "recommendation_type", length = 100)
    private String recommendationType;

    @Column(columnDefinition = "TEXT", name = "recommendation_type_other")
    private String recommendationTypeOther;

    @Column(name = "recommended_social_worker", length = 150)
    private String recommendedSocialWorker;

    @Column(name = "external_representative_name", length = 150)
    private String externalRepresentativeName;

    @Column(name = "external_representative_office", length = 100)
    private String externalRepresentativeOffice;

    @Column(name = "external_representative_position", length = 100)
    private String externalRepresentativePosition;

    @Column(name = "external_representative_contact", length = 50)
    private String externalRepresentativeContact;

    @Column(name = "recommendation_date")
    private LocalDate recommendationDate;

    @Column(columnDefinition = "TEXT", name = "recommended_family_support")
    private String recommendedFamilySupport;

    @Column(columnDefinition = "TEXT", name = "recommended_psychosocial_action")
    private String recommendedPsychosocialAction;

    @Column(name = "date_received")
    private LocalDate dateReceived;

    public boolean isAcceptedByAgency() {
        return Boolean.TRUE.equals(this.acceptedByAgency);
    }

    public boolean isDeclinedByAgency() {
        return Boolean.TRUE.equals(this.declinedByAgency);
    }
}

