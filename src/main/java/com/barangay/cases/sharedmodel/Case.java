package com.barangay.cases.sharedmodel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.barangay.cases.lupon.model.LuponReferral;
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

@Entity
@Table(name = "cases")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Case extends BaseEntity {

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

    @Column(name = "is_archived", nullable = false)
    private Boolean isArchived = false;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCaseNumber() { return caseNumber; }
    public void setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; }

    public FilingType getFilingType() { return filingType; }
    public void setFilingType(FilingType filingType) { this.filingType = filingType; }

    public String getPreviousCaseNumber() { return previousCaseNumber; }
    public void setPreviousCaseNumber(String previousCaseNumber) { this.previousCaseNumber = previousCaseNumber; }

    public String getReOpenedCaseNumber() { return reOpenedCaseNumber; }
    public void setReOpenedCaseNumber(String reOpenedCaseNumber) { this.reOpenedCaseNumber = reOpenedCaseNumber; }

    public Integer getSummonCount() { return summonCount; }
    public void setSummonCount(Integer summonCount) { this.summonCount = summonCount; }

    public CaseStatus getCaseStatus() { return caseStatus; }
    public void setCaseStatus(CaseStatus caseStatus) { this.caseStatus = caseStatus; }

    public String getCaseStatusRemarks() { return caseStatusRemarks; }
    public void setCaseStatusRemarks(String caseStatusRemarks) { this.caseStatusRemarks = caseStatusRemarks; }

    public Boolean getIsArchived() { return isArchived; }
    public void setIsArchived(Boolean isArchived) { this.isArchived = isArchived; }

    public Boolean getIsAttested() { return isAttested; }
    public void setIsAttested(Boolean isAttested) { this.isAttested = isAttested; }

    public LocalDateTime getAttestedAt() { return attestedAt; }
    public void setAttestedAt(LocalDateTime attestedAt) { this.attestedAt = attestedAt; }

    public String getSettlementTerms() { return settlementTerms; }
    public void setSettlementTerms(String settlementTerms) { this.settlementTerms = settlementTerms; }

    public LocalDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDateTime settledAt) { this.settledAt = settledAt; }

    public byte[] getSettlementDocument() { return settlementDocument; }
    public void setSettlementDocument(byte[] settlementDocument) { this.settlementDocument = settlementDocument; }

    public String getSettlementVenue() { return settlementVenue; }
    public void setSettlementVenue(String settlementVenue) { this.settlementVenue = settlementVenue; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getDangerAssessments() { return dangerAssessments; }
    public void setDangerAssessments(String dangerAssessments) { this.dangerAssessments = dangerAssessments; }

    public String getImmediateRequests() { return immediateRequests; }
    public void setImmediateRequests(String immediateRequests) { this.immediateRequests = immediateRequests; }

    public Complainant getComplainant() { return complainant; }
    public void setComplainant(Complainant complainant) { this.complainant = complainant; }

    public Respondent getRespondent() { return respondent; }
    public void setRespondent(Respondent respondent) { this.respondent = respondent; }

    public NarrativeStatement getNarrativeStatement() { return narrativeStatement; }
    public void setNarrativeStatement(NarrativeStatement narrativeStatement) { this.narrativeStatement = narrativeStatement; }

    public IncidentDetail getIncidentDetail() { return incidentDetail; }
    public void setIncidentDetail(IncidentDetail incidentDetail) { this.incidentDetail = incidentDetail; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public CertificateToFileAction getCertificateToFileAction() { return certificateToFileAction; }
    public void setCertificateToFileAction(CertificateToFileAction certificateToFileAction) { this.certificateToFileAction = certificateToFileAction; }

    public List<Witness> getWitnesses() { return witnesses; }
    public void setWitnesses(List<Witness> witnesses) { this.witnesses = witnesses; }

    public LuponReferral getLuponReferral() { return luponReferral; }
    public void setLuponReferral(LuponReferral luponReferral) { this.luponReferral = luponReferral; }

    public List<ChildInvolved> getChildren() { return children; }
    public void setChildren(List<ChildInvolved> children) { this.children = children; }

    public Officer getOfficer() { return officer; }
    public void setOfficer(Officer officer) { this.officer = officer; }

    public User getCaseFilledBy() { return caseFilledBy; }
    public void setCaseFilledBy(User caseFilledBy) { this.caseFilledBy = caseFilledBy; }

    public User getCaseUpdatedBy() { return caseUpdatedBy; }
    public void setCaseUpdatedBy(User caseUpdatedBy) { this.caseUpdatedBy = caseUpdatedBy; }
}
