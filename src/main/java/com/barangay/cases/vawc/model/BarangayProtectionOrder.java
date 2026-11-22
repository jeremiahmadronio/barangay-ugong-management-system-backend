package com.barangay.cases.vawc.model;


import com.barangay.cases.shared.model.Case;
import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.entity.BaseEntity;
import com.barangay.zshared.enumerated.BpoStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "barangay_protection_orders", indexes = {
        @Index(name = "idx_bpo_case_id", columnList = "case_id"),
        @Index(name = "idx_bpo_control_number", columnList = "bpo_control_number"),
        @Index(name = "idx_bpo_status", columnList = "status"),
        @Index(name = "idx_bpo_activated_at", columnList = "activated_at"),
        @Index(name = "idx_bpo_expired_at", columnList = "expired_at")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BarangayProtectionOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "case_id" , nullable = false)
    private Case cases;

    @Column(name = "bpo_control_number", unique = true, nullable = false, length = 100)
    private String bpoControlNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BpoStatus status = BpoStatus.PENDING;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "expired_at")
    private LocalDate expiredAt;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;



    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "violence_type_bridge",
            joinColumns = @JoinColumn(name = "bpo_id"),
            inverseJoinColumns = @JoinColumn(name = "violence_type_id")
    )
    private Set<ViolenceType> violenceTypes = new HashSet<>();

    @Column(name = "served_by")
    private String servedBy;

    @Column(name = "date_served")
    private LocalDateTime dateServed;

    @Column(name = "service_method")
    private String serviceMethod;

    @Column(name = "witness1_name")
    private String witness1Name;

    @Column(name = "witness2_name")
    private String witness2Name;

    @Column(name = "certification_file_data", columnDefinition = "TEXT")
    private String certificationOfServiceFileData;
}
