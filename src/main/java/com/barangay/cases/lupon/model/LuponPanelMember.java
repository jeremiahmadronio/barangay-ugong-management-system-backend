package com.barangay.cases.lupon.model;

import com.barangay.cases.shared.model.Case;
import com.barangay.people.officer.model.Officer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "lupon_panel_members", indexes = {
        @Index(name = "idx_lupon_panel_case_id", columnList = "case_id"),
        @Index(name = "idx_lupon_panel_officer_id", columnList = "officer_id"),
        @Index(name = "idx_lupon_panel_position", columnList = "panel_position")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LuponPanelMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case cases;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "officer_id", nullable = false)
    private Officer officer;

    @Column(name = "panel_position", length = 50, nullable = false)
    private String position; // e.g., "CHAIRMAN", "SECRETARY", "MEMBER"

    @CreationTimestamp
    @Column(name = "assigned_at", updatable = false, nullable = false)
    private LocalDateTime assignedAt;
}