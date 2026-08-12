package com.barangay.cases.lupon.model;

import com.barangay.cases.shared.model.Case;
import com.barangay.zshared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "lupon_referrals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LuponReferral extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false, unique = true)
    private Case cases;

    @CreationTimestamp
    @Column(name = "referred_at", updatable = false, nullable = false)
    private LocalDateTime referredAt;

    @Column(name = "initial_deadline", nullable = false)
    private LocalDateTime initialDeadline;

    @Column(name = "extended_deadline")
    private LocalDateTime extendedDeadline;

    @Column(name = "extension_granted_at")
    private LocalDateTime extensionGrantedAt;

    @Column(name = "extension_reason", columnDefinition = "TEXT")
    private String extensionReason;

    @Column(name = "extension_count", nullable = false)
    private Integer extensionCount = 0;
}