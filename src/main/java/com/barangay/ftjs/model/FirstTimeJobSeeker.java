package com.barangay.ftjs.model;

import com.barangay.people.person.model.Person;
import com.barangay.people.resident.model.Resident;
import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.entity.Archivable;
import com.barangay.zshared.entity.BaseEntity;
import com.barangay.zshared.enumerated.FtjsStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "first_time_job_seeker", indexes = {
        @Index(name = "idx_ftjs_resident_id", columnList = "resident_id"),
        @Index(name = "idx_ftjs_person_id", columnList = "person_id"),
        @Index(name = "idx_ftjs_number", columnList = "ftjs_number"),
        @Index(name = "idx_ftjs_status_deleted", columnList = "status, is_deleted, is_archived"),
        @Index(name = "idx_ftjs_trend_lookup", columnList = "is_deleted, created_at, status, educational_attainment")
})
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@SQLDelete(sql = "UPDATE first_time_job_seeker SET is_deleted = true, deleted_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class FirstTimeJobSeeker extends BaseEntity implements Archivable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id")
    private Resident resident;

    @Column(name = "ftjs_number", columnDefinition = "TEXT")
    private String ftjsNumber;

    @Column(name = "educational_attainment", length = 100)
    private String educationalAttainment;

    private String course;

    @Column(name = "school_institution")
    private String schoolInstitution;

    private String schoolAddress;

    @Column(name = "valid_id_type")
    private String validIdType;


    @Column(columnDefinition = "TEXT")
    private String purposeDocuments;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "valid_id_files")
    private byte[] validIdFiles;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "oath_files")
    private byte[] oathFiles;

    @Column(name = "issuance_count")
    private Integer issuanceCount;

    private LocalDate dateSubmitted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private FtjsStatus status;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "is_archived")
    private Boolean isArchived = false;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "archive_remarks")
    private String archiveRemarks;
}
