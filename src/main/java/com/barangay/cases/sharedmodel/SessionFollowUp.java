package com.barangay.cases.sharedmodel;

import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "session_follow_up")
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
