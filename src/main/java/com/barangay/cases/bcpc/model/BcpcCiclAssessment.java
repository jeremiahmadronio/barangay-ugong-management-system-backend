package com.barangay.cases.bcpc.model;

import com.barangay.zshared.entity.BaseEntity;
import com.barangay.zshared.enumerated.CiclPathway;
import com.barangay.zshared.enumerated.DiscernmentStatus;
import com.barangay.zshared.enumerated.DiversionAuthority;
import com.barangay.zshared.enumerated.PenaltyRoutingCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Child in Conflict with the Law (CICL) Assessment record under RA 9344 / RA 10630.
 * Tracks age exemption tier, discernment assessment by LSWDO, diversion eligibility, and apprehension records.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "bcpc_cicl_assessments")
public class BcpcCiclAssessment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bcpc_case_details_id", nullable = false, unique = true)
    private BcpcCaseDetails bcpcCaseDetails;

    @Column(name = "age_at_incident", nullable = false)
    private Integer ageAtIncident;

    @Enumerated(EnumType.STRING)
    @Column(name = "cicl_pathway", length = 30)
    private CiclPathway ciclPathway = CiclPathway.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "discernment_status", length = 40)
    private DiscernmentStatus discernmentStatus = DiscernmentStatus.PENDING_LSWDO_EVALUATION;

    @Enumerated(EnumType.STRING)
    @Column(name = "penalty_routing_category", length = 30)
    private PenaltyRoutingCategory penaltyRoutingCategory = PenaltyRoutingCategory.UNDETERMINED;

    @Enumerated(EnumType.STRING)
    @Column(name = "diversion_authority", length = 30)
    private DiversionAuthority diversionAuthority = DiversionAuthority.UNDETERMINED;

    @Column(name = "is_diversion_eligible")
    private Boolean isDiversionEligible;

    @Column(name = "admitted_offense")
    private Boolean admittedOffense = false;

    @Column(name = "is_repeat_offender")
    private Boolean isRepeatOffender = false;

    @Column(name = "alleged_violation", length = 255)
    private String allegedViolation;

    @Column(name = "apprehending_officer_name", length = 150)
    private String apprehendingOfficerName;

    @Column(name = "apprehending_officer_office", length = 150)
    private String apprehendingOfficerOffice;

    @Column(name = "apprehending_officer_contact", length = 50)
    private String apprehendingOfficerContact;

    @Column(name = "apprehension_datetime")
    private LocalDateTime apprehensionDateTime;

    @Column(name = "apprehension_place", length = 255)
    private String apprehensionPlace;
}

