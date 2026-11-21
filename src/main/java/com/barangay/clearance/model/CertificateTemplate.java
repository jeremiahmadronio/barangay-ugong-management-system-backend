package com.barangay.clearance.model;

import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.entity.Archivable;
import com.barangay.zshared.entity.BaseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "certificate_template", indexes = {
        @Index(name = "idx_cert_template_title", columnList = "certTitle"),
        @Index(name = "idx_cert_template_archived", columnList = "is_archived")
})
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class CertificateTemplate extends BaseEntity implements Archivable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String certTitle;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode bodySections;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode issueFields;

    private boolean requiresPhoto = false;

    private boolean requiresThumbmark = false;

    @Column(name = "cert_fee", precision = 12, scale = 2)
    private BigDecimal certFee = BigDecimal.ZERO;

    private Integer validityMonths = 6;

    @Column(length = 80)
    private String footerText = "Not valid without dry seal.";

    @Column(name = "is_archived")
    private Boolean isArchived = false;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "archive_remarks", length = 250)
    private String archiveRemarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL)
    private List<IssuedCertificate> issuedCertificates;
}
