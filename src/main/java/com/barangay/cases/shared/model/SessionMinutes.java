package com.barangay.cases.shared.model;

import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.entity.BaseEntity;
import com.barangay.zshared.enumerated.SessionOutcome;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "session_minutes", indexes = {
        @Index(name = "idx_session_minutes_session_id", columnList = "session_id")
})
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class SessionMinutes extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private CaseSession session;

    @Column(name = "is_complainant_present")
    private Boolean complainantPresent = false;

    @Column(name = "is_respondent_present")
    private Boolean respondentPresent = false;

    @Column(columnDefinition = "TEXT", name = "session_notes")
    private String hearingNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome")
    private SessionOutcome outcome;

    @Column(columnDefinition = "TEXT", name = "settlement_terms")
    private String settlementTerms;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User recordedBy;


}

