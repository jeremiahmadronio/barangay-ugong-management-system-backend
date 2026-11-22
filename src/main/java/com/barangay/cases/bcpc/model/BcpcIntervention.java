package com.barangay.cases.bcpc.model;

import com.barangay.zshared.entity.BaseEntity;
import com.barangay.zshared.enumerated.BcpcInterventionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Tracks individual or group intervention, diversion contract, or rehabilitation programs
 * designed for the child (applicable to both CAR and CICL).
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
    name = "bcpc_interventions",
    indexes = {
        @Index(name = "idx_bcpc_interventions_case_details_id", columnList = "bcpc_case_details_id"),
        @Index(name = "idx_bcpc_interventions_status", columnList = "status")
    }
)
public class BcpcIntervention extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bcpc_case_details_id", nullable = false)
    private BcpcCaseDetails bcpcCaseDetails;

    @Enumerated(EnumType.STRING)
    @Column(name = "intervention_type", nullable = false, length = 50)
    private BcpcInterventionType interventionType;

    @Column(name = "program_title", nullable = false, length = 150)
    private String programTitle;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "ONGOING"; // ONGOING, COMPLETED, TERMINATED, REVISED

    @Column(name = "facilitator_or_agency", length = 150)
    private String facilitatorOrAgency;

    @Column(name = "progress_notes", columnDefinition = "TEXT")
    private String progressNotes;
}

