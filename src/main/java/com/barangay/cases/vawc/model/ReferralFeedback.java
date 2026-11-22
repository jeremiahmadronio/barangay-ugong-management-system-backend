package com.barangay.cases.vawc.model;

import com.barangay.cases.shared.model.Case;
import com.barangay.zshared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "vawc_referral_feedback", indexes = {
        @Index(name = "idx_vawc_feedback_case_id", columnList = "case_id"),
        @Index(name = "idx_vawc_feedback_referral_id", columnList = "vawc_referral_id")
})
@NoArgsConstructor
@AllArgsConstructor
public class ReferralFeedback extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = true)
    private Case cases;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vawc_referral_id", nullable = true)
    private VawcReferral vawcReferral;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String details;

    @Column(name = "resolves_case", nullable = false)
    private Boolean resolvesCase = false;

    @Column(name = "resolution_type")
    private String resolutionType;

    @Column(name = "proof_file_data", columnDefinition = "TEXT")
    private String proofFileData;

    @Column(name = "proof_file_name")
    private String proofFileName;

    @Column(name = "declined_by_agency", nullable = false)
    private Boolean declinedByAgency = false;

    @Column(name = "reason_for_decline", columnDefinition = "TEXT")
    private String reasonForDecline;
}

