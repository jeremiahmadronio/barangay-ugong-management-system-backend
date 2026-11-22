package com.barangay.cases.vawc.model;

import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vawc_intervention_follow_ups", indexes = {
        @Index(name = "idx_vawc_followup_intervention_id", columnList = "intervention_id")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class InterventionFollowUp extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intervention_id", nullable = false)
    private Intervention  intervention;

    @Column(columnDefinition = "TEXT", name = "notes")
    private String notes;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by" , nullable = false)
    private User createdBy;

}
