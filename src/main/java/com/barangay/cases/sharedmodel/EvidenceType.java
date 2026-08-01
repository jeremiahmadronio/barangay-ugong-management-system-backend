package com.barangay.cases.sharedmodel;

import com.barangay.zshared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "evidence_types")
@Getter
@Setter
public class EvidenceType extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "file")
    byte[] file;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}