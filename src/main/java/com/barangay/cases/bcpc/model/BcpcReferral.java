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
 * (e.g. LSWDO, DSWD, Bahay Pag-asa, PNP WCPC, RJJWC, or Family Court).
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

    @Column(name = "purpose", length = 150)
    private String purpose;

    @Column(columnDefinition = "TEXT", name = "grounds")
    private String grounds;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "Active"; // Active, Resolved, Declined

    @Column(name = "referred_by", length = 150)
    private String referredBy;

    @Column(name = "referral_date", nullable = false)
    private LocalDateTime referralDate;
}

