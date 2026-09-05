package com.barangay.cases.shared.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.barangay.cases.lupon.model.LuponReferral;
import com.barangay.cases.vawc.model.BarangayProtectionOrder;
import com.barangay.cases.vawc.model.ChildInvolved;
import com.barangay.people.complainant.model.Complainant;
import com.barangay.people.officer.model.Officer;
import com.barangay.people.respondent.model.Respondent;
import com.barangay.people.witness.model.Witness;
import com.barangay.usermanagement.department.model.Department;
import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.entity.BaseEntity;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.FilingType;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.barangay.zshared.entity.Archivable;
import com.barangay.zshared.entity.BaseEntity;

@Entity
@Table(name = "cases")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Case extends BaseEntity implements Archivable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_number", unique = true, length = 50)
    private String caseNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "filing_type",nullable = false)
    private FilingType filingType;

    @Column(name = "previous_case_number", length = 50)
    private String previousCaseNumber;

    @Column(name = "reopened_case_number", length = 50)
    private String reOpenedCaseNumber;

    @Column(name = "summon_count")
    private Integer summonCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "case_status", nullable = false)
    private CaseStatus caseStatus = CaseStatus.PENDING;

    @Column(columnDefinition = "TEXT", name = "case_status_remarks")
    private String caseStatusRemarks;

    @Column(columnDefinition = "TEXT", name = "reopen_remarks")
    private String reopenRemarks;

    @Column(name = "is_archived", nullable = false)
    private Boolean isArchived = false;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(columnDefinition = "TEXT", name = "archive_remarks")
    private String archiveRemarks;

    @Column(name = "is_attested")
    private Boolean isAttested = false;

    @Column(name = "attested_at")
    private LocalDateTime attestedAt;

    @Column(columnDefinition = "TEXT" ,name = "settlement_terms" )
    private String settlementTerms;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "settlement_document")
    private byte[] settlementDocument;

    @Column(name = "settlement_venue")
    private String settlementVenue;

    @Column(name = "risk_level")
    private String riskLevel;

    @Column(name = "danger_assessments", columnDefinition = "TEXT")
    private String dangerAssessments;

    @Column(name = "immediate_requests", columnDefinition = "TEXT")
    private String immediateRequests;

    @OneToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    @JoinColumn(name = "complainant_id")
    private Complainant complainant;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "respondent_id")
    private Respondent respondent;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "narrative_statemet_id")
    private NarrativeStatement narrativeStatement;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_detail_id")
    private IncidentDetail incidentDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @OneToOne(mappedBy = "cases", cascade = CascadeType.ALL)
    private CertificateToFileAction certificateToFileAction;

    @OneToMany(mappedBy = "aCase", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Witness> witnesses = new ArrayList<>();

    @OneToOne(mappedBy = "cases", cascade = CascadeType.ALL)
    private LuponReferral luponReferral;

    @OneToOne(mappedBy = "cases", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private BarangayProtectionOrder barangayProtectionOrder;

    @OneToMany(mappedBy = "cases", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ChildInvolved> children = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_assign_to")
    private Officer officer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_filed_by")
    private User caseFilledBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_updated_by")
    private User caseUpdatedBy;

    @Column(name = "is_extended")
    private Boolean isExtended = false;

    @Column(name = "extension_count")
    private Integer extensionCount = 0;

    @Column(name = "extended_at")
    private LocalDateTime extendedAt;

    @Column(columnDefinition = "TEXT", name = "extension_reason")
    private String extensionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "extended_by")
    private User extendedBy;
}
