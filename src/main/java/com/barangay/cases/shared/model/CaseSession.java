package com.barangay.cases.shared.model;

import com.barangay.zshared.entity.BaseEntity;
import com.barangay.zshared.enumerated.SessionStatus;
import com.barangay.zshared.enumerated.SessionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "case_sessions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_case_session_summon",
                columnNames = {"case_id", "summon_number"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CaseSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case cases;

    @Enumerated(EnumType.STRING)
    @Column(name = "hearing_type", nullable = false)
    private SessionType hearingType = SessionType.MEDIATION;

    @Column(name = "summon_number", nullable = false)
    private Short summonNumber;

    @Column(name = "scheduled_start", nullable = false)
    private LocalDateTime scheduledStart;

    @Column(name = "scheduled_end", nullable = false)
    private LocalDateTime scheduledEnd;

    @Column(name = "venue", length = 255)
    private String venue = "Barangay Hall";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SessionStatus status = SessionStatus.SCHEDULED;

    @Column(name = "notice_generated_at")
    private LocalDateTime noticeGeneratedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<SessionFollowUp> followUps = new ArrayList<>();
}