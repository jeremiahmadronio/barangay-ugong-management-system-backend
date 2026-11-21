package com.barangay.clearance.model;

import com.barangay.people.person.model.Person;
import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.entity.Archivable;
import com.barangay.zshared.entity.BaseEntity;
import com.barangay.zshared.enumerated.ClearanceStatus;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "issued_certificate", indexes = {
        @Index(name = "idx_issued_cert_number", columnList = "certNumber"),
        @Index(name = "idx_issued_cert_template_id", columnList = "template_id"),
        @Index(name = "idx_issued_cert_person_id", columnList = "person_id"),
        @Index(name = "idx_issued_cert_status", columnList = "status"),
        @Index(name = "idx_issued_cert_archived", columnList = "is_archived"),
        @Index(name = "idx_issued_cert_expiry_date", columnList = "expiryDate"),
        @Index(name = "idx_issued_cert_created_at", columnList = "created_at")
})
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class IssuedCertificate extends BaseEntity implements Archivable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String certNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private CertificateTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    private Person person;

    private String requestorName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode fieldValues;

    @Column(length = 50)
    private String orNumber;

    @Column(length = 50)
    private String ctnNumber;

    @Column(precision = 12, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "is_free", nullable = false)
    private boolean isFree = false;

    private String signatoryName;

    private String signatoryPosition;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ClearanceStatus status = ClearanceStatus.ACTIVE;

    private LocalDate expiryDate;

    @Column(name = "is_archived")
    private Boolean isArchived = false;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "archive_remarks", length = 250)
    private String archiveRemarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by_id")
    private User issuedBy;
}
