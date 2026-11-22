package com.barangay.cases.bcpc.model;

import com.barangay.zshared.entity.BaseEntity;
import com.barangay.zshared.enumerated.CarActCategory;
import com.barangay.zshared.enumerated.CarCaseBasis;
import com.barangay.zshared.enumerated.CarOccurrenceCategory;
import com.barangay.zshared.enumerated.SpecificCarAct;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Child at Risk (CAR) Assessment record under Chapter V of RA 9344 / RA 10630.
 * Holds protection needs, vulnerability analysis, and shelter placement.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
    name = "bcpc_car_assessments",
    indexes = {
        @Index(name = "idx_bcpc_car_assessments_case_details_id", columnList = "bcpc_case_details_id")
    }
)
public class BcpcCarAssessment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bcpc_case_details_id", nullable = false, unique = true)
    private BcpcCaseDetails bcpcCaseDetails;

    @Enumerated(EnumType.STRING)
    @Column(name = "car_case_basis", length = 50)
    private CarCaseBasis carCaseBasis = CarCaseBasis.CHAPTER_V_CAR;

    @Enumerated(EnumType.STRING)
    @Column(name = "car_act_category", length = 50)
    private CarActCategory carActCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "specific_car_act", length = 60)
    private SpecificCarAct specificCarAct;

    @Column(name = "specific_car_act_other", length = 150)
    private String specificCarActOther;

    @Enumerated(EnumType.STRING)
    @Column(name = "car_occurrence_category", length = 30)
    private CarOccurrenceCategory carOccurrenceCategory = CarOccurrenceCategory.FIRST_OCCURRENCE;

    @Column(name = "needs_protective_custody")
    private Boolean needsProtectiveCustody = false;

    @Column(name = "temporary_shelter_placement", length = 150)
    private String temporaryShelterPlacement;

    @Column(name = "risk_level", length = 30)
    private String riskLevel; // LOW, MODERATE, HIGH

    @Column(name = "high_risk_indicators", columnDefinition = "TEXT")
    private String highRiskIndicators;

    @Column(name = "safety_and_welfare_notes", columnDefinition = "TEXT")
    private String safetyAndWelfareNotes;

    @Column(name = "needs_medical_attention")
    private Boolean needsMedicalAttention = false;
}
