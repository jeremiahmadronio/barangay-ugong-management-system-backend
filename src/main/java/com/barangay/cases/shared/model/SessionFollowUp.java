package com.barangay.cases.shared.model;

import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "session_follow_up", indexes = {
        @Index(name = "idx_session_follow_up_session_id", columnList = "session_id")
})
@Getter
@Setter
@NoArgsConstructor @AllArgsConstructor
public class SessionFollowUp extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private CaseSession session;

    @Column(columnDefinition = "TEXT", nullable = false,name = "remarks")
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User recordedBy;


}
