package com.barangay.cases.bcpc.model;

import com.barangay.cases.shared.model.Case;
import com.barangay.people.person.model.Person;
import com.barangay.zshared.entity.BaseEntity;
import com.barangay.zshared.enumerated.BcpcCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Core intake profile entity for cases managed under Barangay Council for the Protection of Children (BCPC).
 * Links directly to the universal Case entity and references the child Person as the single source of truth.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "bcpc_case_details")
public class BcpcCaseDetails extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false, unique = true)
    private Case cases;

    @Enumerated(EnumType.STRING)
    @Column(name = "bcpc_category", nullable = false, length = 20)
    private BcpcCategory category;

    /**
     * The child involved in the case (either the Child at Risk or Child in Conflict with the Law).
     * All personal details (name, gender, birthDate, address, contact) are fetched from this Person record.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_person_id", nullable = false)
    private Person child;

    @Column(name = "grade_level", length = 100)
    private String gradeLevel;

    @Column(name = "school_name", length = 255)
    private String schoolName;

    /**
     * Parent, legal guardian, or adult custodian responsible for the child during intake.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_person_id")
    private Person guardian;

    @Column(name = "guardian_relationship", length = 100)
    private String guardianRelationship;

    @Column(name = "is_guardian_present_at_intake")
    private Boolean isGuardianPresentAtIntake = true;

    @Column(name = "guardian_unavailable_reason", columnDefinition = "TEXT")
    private String guardianUnavailableReason;

    @Column(name = "complainant_role", length = 100)
    private String complainantRole;

    /**
     * CAR-specific assessment details. Populated only when category == CAR.
     */
    @OneToOne(mappedBy = "bcpcCaseDetails", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private BcpcCarAssessment carAssessment;

    /**
     * CICL-specific assessment details. Populated only when category == CICL.
     */
    @OneToOne(mappedBy = "bcpcCaseDetails", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private BcpcCiclAssessment ciclAssessment;

    /**
     * Active and historical interventions assigned to the child.
     */
    @OneToMany(mappedBy = "bcpcCaseDetails", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<BcpcIntervention> interventions = new ArrayList<>();

    // --- Custody & Turnover Tracking ---
    @Column(name = "custody_status", length = 50)
    private String custodyStatus;

    @Column(name = "temporary_placement", length = 150)
    private String temporaryPlacement;

    @Column(name = "initial_responsibility", columnDefinition = "TEXT")
    private String initialResponsibility;

    @Column(name = "contact_number", length = 50)
    private String contactNumber;

    @Column(name = "transfer_date")
    private java.time.LocalDate transferDate;

    @Column(name = "transfer_time", length = 20)
    private String transferTime;

    @Column(name = "temporary_placement_reason", columnDefinition = "TEXT")
    private String temporaryPlacementReason;

    @Column(name = "transfer_notes", columnDefinition = "TEXT")
    private String transferNotes;

    @Column(name = "needs_medical_attention")
    private Boolean needsMedicalAttention;

    @Column(name = "health_coordination_confirmed")
    private Boolean healthCoordinationConfirmed;

    @Column(name = "child_guidance_provided")
    private Boolean childGuidanceProvided;

    @Column(name = "parent_advice_provided")
    private Boolean parentAdviceProvided;

    @Column(name = "guidance_notes", columnDefinition = "TEXT")
    private String guidanceNotes;

    // --- CICL Specific Custody Fields ---
    @Column(name = "was_apprehended")
    private Boolean wasApprehended;

    @Column(name = "custody_resolution", length = 100)
    private String custodyResolution;

    @Column(name = "cicl_turnover_date")
    private String turnoverDate;

    @Column(name = "cicl_turnover_time", length = 20)
    private String turnoverTime;

    @Column(name = "turned_over_to", length = 150)
    private String turnedOverTo;

    @Column(name = "turned_over_to_specify", length = 150)
    private String turnedOverToSpecify;

    @Column(name = "delay_reason", columnDefinition = "TEXT")
    private String delayReason;

    @Column(name = "custody_remarks", columnDefinition = "TEXT")
    private String custodyRemarks;
}
