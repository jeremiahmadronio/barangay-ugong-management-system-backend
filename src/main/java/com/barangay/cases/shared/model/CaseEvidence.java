package com.barangay.cases.shared.model;

import com.barangay.zshared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import java.sql.Types;

@Entity
@Table(name = "case_evidences", indexes = {
        @Index(name = "idx_case_evidence_case_id", columnList = "case_id"),
        @Index(name = "idx_case_evidence_type_id", columnList = "evidence_type_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CaseEvidence extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case cases;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evidence_type_id", nullable = false)
    private EvidenceType type;

    @JdbcTypeCode(Types.VARBINARY)
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "files", columnDefinition = "bytea")
    private byte[] files;

    @Column(name = "fileName")
    private String fileName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
