package com.barangay.cases.vawc.model;

import com.barangay.cases.shared.model.Case;
import com.barangay.zshared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "vawc_referrals", indexes = {
        @Index(name = "idx_vawc_referral_case_id", columnList = "case_id"),
        @Index(name = "idx_vawc_referral_control_no", columnList = "referral_control_number"),
        @Index(name = "idx_vawc_referral_status", columnList = "status"),
        @Index(name = "idx_vawc_referral_date", columnList = "referral_date")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VawcReferral extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case cases;

    @Column(name = "referral_control_number", unique = true, length = 100)
    private String referralControlNumber;

    @Column(name = "referred_to", nullable = false)
    private String referredTo;

    @Column(name = "subject_of_litigations")
    private String subjectOfLitigations;

    @Column(columnDefinition = "TEXT", name = "grounds")
    private String grounds;

    @Column(name = "status", nullable = false)
    private String status = "Active"; // Active, Resolved

    @Column(name = "referred_by")
    private String referredBy;

    @Column(name = "referral_date", nullable = false)
    private LocalDateTime referralDate;

}
