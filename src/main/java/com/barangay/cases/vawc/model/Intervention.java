package com.barangay.cases.vawc.model;

import com.barangay.cases.shared.model.Case;
import com.barangay.people.officer.model.Officer;
import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "vawc_interventions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Intervention extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case cases;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bpo_id", nullable = true)
    private BarangayProtectionOrder barangayProtectionOrder;

    @Column(name = "activity_type", length = 200, nullable = false)
    private String activityType;

    @Column(name = "intervention_details", columnDefinition = "TEXT")
    private String interventionDetails;

    @Column(name = "intervention_date", nullable = false)
    private LocalDateTime interventionDate;

    @Column(name = "intervention_duration")
    private Integer interventionDuration;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "intervention_performed_by",
            joinColumns = @JoinColumn(name = "intervention_id"),
            inverseJoinColumns = @JoinColumn(name = "officer_id")
    )
    private Set<Officer> performedBy = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "intervention", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InterventionFollowUp> followUps = new ArrayList<>();
}

