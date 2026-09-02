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

/**
 * Entity representing an issued barangay clearance or certificate.
 * Contains applicant demographic information, filled form values, payment details, and signatory snapshot.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "issued_certificate")
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class IssuedCertificate extends BaseEntity implements Archivable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /** Unique certificate reference number (e.g., "BC-2026-0001"). */
    @Column(unique = true, nullable = false, length = 50)
    private String certNumber;

    /** Certificate template used for this issuance. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private CertificateTemplate template;

    /** Linked resident/person record in the system directory. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    private Person person;

    /** Full name of the applicant or requestor. */
    private String requestorName;


    /** Dynamic field values submitted for this certificate (stored as JSONB). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode fieldValues;


    /** Official Receipt (OR) Number issued for the transaction. */
    @Column(length = 50)
    private String orNumber;

    /** Community Tax Certificate (CTC / Cedula) Number, if provided. */
    @Column(length = 50)
    private String ctnNumber;

    /** Amount paid for this certificate issuance (defaults to ZERO). */
    @Column(precision = 12, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    /** Indicates whether the certificate fee was waived (defaults to false). */
    @Column(name = "is_free", nullable = false)
    private boolean isFree = false;


    /** Full name of the signing authority at the time of issuance. */
    private String signatoryName;

    /** Official position/title of the signing authority (e.g., "PUNONG BARANGAY"). */
    private String signatoryPosition;


    /** Lifecycle status of the issued certificate (ACTIVE, EXPIRED, VOID). */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ClearanceStatus status = ClearanceStatus.ACTIVE;

    /** Expiration date of the clearance validity period. */
    private LocalDate expiryDate;


    /** Indicates whether the record has been archived. */
    @Column(name = "is_archived")
    private Boolean isArchived = false;

    /** Timestamp when the record was archived. */
    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    /** Remarks or justification for archiving/voiding the certificate. */
    @Column(name = "archive_remarks", length = 250)
    private String archiveRemarks;


    /** Barangay officer or staff member who processed and issued the certificate. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by_id")
    private User issuedBy;
}
