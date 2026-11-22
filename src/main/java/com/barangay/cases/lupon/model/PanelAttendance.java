package com.barangay.cases.lupon.model;

import com.barangay.cases.shared.model.CaseSession;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "panel_attendances", indexes = {
        @Index(name = "idx_panel_att_session_id", columnList = "session_id"),
        @Index(name = "idx_panel_att_member_id", columnList = "panel_member_id"),
        @Index(name = "idx_panel_att_session_member", columnList = "session_id, panel_member_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PanelAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private CaseSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "panel_member_id", nullable = false)
    private LuponPanelMember panelMember;

    @Column(name = "is_present", nullable = false)
    private Boolean isPresent = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;
}