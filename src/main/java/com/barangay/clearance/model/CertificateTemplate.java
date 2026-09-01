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

/**
 * Entity representing a customizable certificate or clearance template.
 * Defines body text structure, required input fields, pricing, and print requirements.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "certificate_template")
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class CertificateTemplate extends BaseEntity implements Archivable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Official title of the certificate (e.g., "BARANGAY CLEARANCE", "BUILDING CLEARANCE"). */
    @Column(unique = true, nullable = false, length = 100)
    private String certTitle;

    /** JSON array of body text sections and paragraph templates with placeholders. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode bodySections;

    /** JSON array of required form field keys (e.g., ["full_name", "purpose", "address"]). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode issueFields;

    /** Whether the printed certificate requires a 2x2 applicant photo box. */
    private boolean requiresPhoto = false;

    /** Whether the printed certificate requires an applicant thumbmark box. */
    private boolean requiresThumbmark = false;

    /** Standard issuance fee for this certificate type. */
    @Column(precision = 12, scale = 2)
    private BigDecimal certFee = BigDecimal.ZERO;

    /** Validity duration in months (defaults to 6 months). */
    private Integer validityMonths = 6;

    /** Footer disclaimer text printed at the bottom of the certificate. */
    @Column(length = 80)
    private String footerText = "Not valid without dry seal.";

    // ── ARCHIVE FIELDS ──

    /** Indicates whether the template is archived and hidden from the issuance menu. */
    @Column(name = "is_archived")
    private Boolean isArchived = false;

    /** Timestamp when the template was archived. */
    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    /** Reason or remarks for archiving this template. */
    @Column(name = "archive_remarks", length = 250)
    private String archiveRemarks;

    // ── AUDIT ──

    /** User account that created or registered this certificate template. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    // ── RELATIONSHIPS ──

    /** List of certificates issued using this template. */
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL)
    private List<IssuedCertificate> issuedCertificates;
}
