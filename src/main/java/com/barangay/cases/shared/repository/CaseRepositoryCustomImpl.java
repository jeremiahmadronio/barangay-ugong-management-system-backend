package com.barangay.cases.shared.repository;

import com.barangay.cases.blotter.dto.BlotterArchiveFilterRequest;
import com.barangay.cases.blotter.dto.BlotterArchiveItemResponse;
import com.barangay.cases.blotter.dto.BlotterFilterRequest;
import com.barangay.cases.blotter.dto.BlotterRecentDeleteItemResponse;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.model.IncidentDetail;
import com.barangay.people.complainant.model.Complainant;
import com.barangay.people.person.model.Person;
import com.barangay.people.respondent.model.Respondent;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.FilingType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CaseRepositoryCustomImpl implements CaseRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Case> findBlotterCasesFiltered(BlotterFilterRequest filter, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // 1. Data Query
        CriteriaQuery<Case> cq = cb.createQuery(Case.class);
        Root<Case> root = cq.from(Case.class);

        List<Predicate> predicates = buildPredicates(filter, cb, root);
        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(root.get("createdAt")));

        TypedQuery<Case> query = entityManager.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Case> resultList = query.getResultList();

        // 2. Count Query
        CriteriaQuery<Long> countCq = cb.createQuery(Long.class);
        Root<Case> countRoot = countCq.from(Case.class);

        List<Predicate> countPredicates = buildPredicates(filter, cb, countRoot);
        countCq.select(cb.count(countRoot));
        countCq.where(countPredicates.toArray(new Predicate[0]));

        Long totalCount = entityManager.createQuery(countCq).getSingleResult();

        return new PageImpl<>(resultList, pageable, totalCount);
    }

    @Override
    public Page<BlotterArchiveItemResponse> findArchivedBlotterCases(BlotterArchiveFilterRequest filter, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // 1. Data Query
        CriteriaQuery<Case> cq = cb.createQuery(Case.class);
        Root<Case> root = cq.from(Case.class);

        List<Predicate> predicates = buildArchivePredicates(filter, cb, root);
        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(cb.coalesce(root.get("archivedAt"), root.get("updatedAt"))));

        TypedQuery<Case> query = entityManager.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Case> cases = query.getResultList();
        List<BlotterArchiveItemResponse> content = cases.stream().map(c -> {
            String compName = "—";
            if (c.getComplainant() != null && c.getComplainant().getPerson() != null) {
                Person p = c.getComplainant().getPerson();
                compName = (p.getLastName() + ", " + p.getFirstName() + (p.getMiddleName() != null && !p.getMiddleName().isBlank() ? " " + p.getMiddleName().charAt(0) + "." : "")).trim();
            }
            String respName = "—";
            if (c.getRespondent() != null && c.getRespondent().getPerson() != null) {
                Person p = c.getRespondent().getPerson();
                respName = (p.getLastName() + ", " + p.getFirstName() + (p.getMiddleName() != null && !p.getMiddleName().isBlank() ? " " + p.getMiddleName().charAt(0) + "." : "")).trim();
            }
            return new BlotterArchiveItemResponse(
                    c.getId(),
                    c.getCaseNumber(),
                    compName,
                    respName,
                    c.getArchiveRemarks() != null ? c.getArchiveRemarks() : "Archived Record",
                    c.getCaseStatus(),
                    c.getArchivedAt() != null ? c.getArchivedAt() : c.getUpdatedAt(),
                    c.getFilingType()
            );
        }).toList();

        // 2. Count Query
        CriteriaQuery<Long> countCq = cb.createQuery(Long.class);
        Root<Case> countRoot = countCq.from(Case.class);
        List<Predicate> countPredicates = buildArchivePredicates(filter, cb, countRoot);
        countCq.select(cb.count(countRoot));
        countCq.where(countPredicates.toArray(new Predicate[0]));

        Long totalCount = entityManager.createQuery(countCq).getSingleResult();

        return new PageImpl<>(content, pageable, totalCount);
    }

    @Override
    public Page<BlotterRecentDeleteItemResponse> findRecentDeletedBlotterCases(BlotterArchiveFilterRequest filter, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // 1. Data Query
        CriteriaQuery<Case> cq = cb.createQuery(Case.class);
        Root<Case> root = cq.from(Case.class);

        List<Predicate> predicates = buildRecentDeletePredicates(filter, cb, root);
        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(cb.coalesce(root.get("deletedAt"), root.get("updatedAt"))));

        TypedQuery<Case> query = entityManager.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Case> cases = query.getResultList();
        List<BlotterRecentDeleteItemResponse> content = cases.stream().map(c -> {
            String compName = "—";
            if (c.getComplainant() != null && c.getComplainant().getPerson() != null) {
                Person p = c.getComplainant().getPerson();
                compName = (p.getLastName() + ", " + p.getFirstName() + (p.getMiddleName() != null && !p.getMiddleName().isBlank() ? " " + p.getMiddleName().charAt(0) + "." : "")).trim();
            }
            String respName = "—";
            if (c.getRespondent() != null && c.getRespondent().getPerson() != null) {
                Person p = c.getRespondent().getPerson();
                respName = (p.getLastName() + ", " + p.getFirstName() + (p.getMiddleName() != null && !p.getMiddleName().isBlank() ? " " + p.getMiddleName().charAt(0) + "." : "")).trim();
            }
            return new BlotterRecentDeleteItemResponse(
                    c.getId(),
                    c.getCaseNumber(),
                    compName,
                    respName,
                    c.getCaseStatus(),
                    c.getDeletedAt() != null ? c.getDeletedAt() : c.getUpdatedAt(),
                    c.getFilingType()
            );
        }).toList();

        // 2. Count Query
        CriteriaQuery<Long> countCq = cb.createQuery(Long.class);
        Root<Case> countRoot = countCq.from(Case.class);
        List<Predicate> countPredicates = buildRecentDeletePredicates(filter, cb, countRoot);
        countCq.select(cb.count(countRoot));
        countCq.where(countPredicates.toArray(new Predicate[0]));

        Long totalCount = entityManager.createQuery(countCq).getSingleResult();

        return new PageImpl<>(content, pageable, totalCount);
    }

    private List<Predicate> buildPredicates(BlotterFilterRequest filter, CriteriaBuilder cb, Root<Case> root) {
        List<Predicate> predicates = new ArrayList<>();

        // Base criteria: non-deleted cases, non-permanently-deleted cases, non-archived cases & root cases only (main cases without previousCaseNumber)
        predicates.add(cb.or(cb.isNull(root.get("isDeleted")), cb.equal(root.get("isDeleted"), false)));
        predicates.add(cb.or(cb.isNull(root.get("isPermanentDeleted")), cb.equal(root.get("isPermanentDeleted"), false)));
        predicates.add(cb.or(cb.isNull(root.get("isArchived")), cb.equal(root.get("isArchived"), false)));
        predicates.add(cb.or(cb.isNull(root.get("previousCaseNumber")), cb.equal(root.get("previousCaseNumber"), "")));

        if (filter == null) {
            return predicates;
        }

        // Search text (Case Number, Complainant First/Last Name, Respondent First/Last Name)
        if (filter.search() != null && !filter.search().isBlank()) {
            String searchPattern = "%" + filter.search().trim().toLowerCase() + "%";

            Join<Case, Complainant> compJoin = root.join("complainant", JoinType.LEFT);
            Join<Complainant, Person> compPersonJoin = compJoin.join("person", JoinType.LEFT);

            Join<Case, Respondent> respJoin = root.join("respondent", JoinType.LEFT);
            Join<Respondent, Person> respPersonJoin = respJoin.join("person", JoinType.LEFT);

            Predicate caseNumberLike = cb.like(cb.lower(root.get("caseNumber")), searchPattern);

            Predicate compFirstNameLike = cb.like(cb.lower(compPersonJoin.get("firstName")), searchPattern);
            Predicate compLastNameLike = cb.like(cb.lower(compPersonJoin.get("lastName")), searchPattern);

            Predicate respFirstNameLike = cb.like(cb.lower(respPersonJoin.get("firstName")), searchPattern);
            Predicate respLastNameLike = cb.like(cb.lower(respPersonJoin.get("lastName")), searchPattern);

            predicates.add(cb.or(caseNumberLike, compFirstNameLike, compLastNameLike, respFirstNameLike, respLastNameLike));
        }

        // Status Filter
        if (filter.status() != null && !filter.status().isBlank() && !"all".equalsIgnoreCase(filter.status())) {
            try {
                CaseStatus statusEnum = CaseStatus.valueOf(filter.status().toUpperCase());
                predicates.add(cb.equal(root.get("caseStatus"), statusEnum));
            } catch (IllegalArgumentException ignored) {
            }
        }

        // Filing Type Filter (FORMAL_COMPLAINT or FOR_RECORD_ONLY)
        if (filter.filingType() != null && !filter.filingType().isBlank() && !"all".equalsIgnoreCase(filter.filingType())) {
            try {
                String typeStr = filter.filingType().toUpperCase();
                if (typeStr.contains("RECORD")) {
                    typeStr = "FOR_RECORD_ONLY";
                }
                FilingType filingEnum = FilingType.valueOf(typeStr);
                predicates.add(cb.equal(root.get("filingType"), filingEnum));
            } catch (IllegalArgumentException ignored) {
            }
        }

        // Nature of Complaint Filter
        if (filter.natureOfComplaint() != null && !filter.natureOfComplaint().isBlank() && !"all".equalsIgnoreCase(filter.natureOfComplaint())) {
            Join<Case, IncidentDetail> incidentJoin = root.join("incidentDetail", JoinType.LEFT);
            String naturePattern = "%" + filter.natureOfComplaint().trim().toLowerCase() + "%";
            predicates.add(cb.like(cb.lower(incidentJoin.get("natureOfComplaint")), naturePattern));
        }

        // Date Range Filter (startDate & endDate)
        if (filter.startDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.startDate().atStartOfDay()));
        }
        if (filter.endDate() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.endDate().atTime(LocalTime.MAX)));
        }

        return predicates;
    }

    private List<Predicate> buildArchivePredicates(BlotterArchiveFilterRequest filter, CriteriaBuilder cb, Root<Case> root) {
        List<Predicate> predicates = new ArrayList<>();

        // Base criteria for Archive Table: isArchived = true, NOT soft-deleted, NOT permanently deleted & root cases only
        predicates.add(cb.equal(root.get("isArchived"), true));
        predicates.add(cb.or(cb.isNull(root.get("isDeleted")), cb.equal(root.get("isDeleted"), false)));
        predicates.add(cb.or(cb.isNull(root.get("isPermanentDeleted")), cb.equal(root.get("isPermanentDeleted"), false)));
        predicates.add(cb.or(cb.isNull(root.get("previousCaseNumber")), cb.equal(root.get("previousCaseNumber"), "")));

        if (filter == null) {
            return predicates;
        }

        if (filter.search() != null && !filter.search().isBlank()) {
            String searchPattern = "%" + filter.search().trim().toLowerCase() + "%";
            Join<Case, Complainant> compJoin = root.join("complainant", JoinType.LEFT);
            Join<Complainant, Person> compPersonJoin = compJoin.join("person", JoinType.LEFT);
            Join<Case, Respondent> respJoin = root.join("respondent", JoinType.LEFT);
            Join<Respondent, Person> respPersonJoin = respJoin.join("person", JoinType.LEFT);

            Predicate caseNumberLike = cb.like(cb.lower(root.get("caseNumber")), searchPattern);
            Predicate compFirstNameLike = cb.like(cb.lower(compPersonJoin.get("firstName")), searchPattern);
            Predicate compLastNameLike = cb.like(cb.lower(compPersonJoin.get("lastName")), searchPattern);
            Predicate respFirstNameLike = cb.like(cb.lower(respPersonJoin.get("firstName")), searchPattern);
            Predicate respLastNameLike = cb.like(cb.lower(respPersonJoin.get("lastName")), searchPattern);

            predicates.add(cb.or(caseNumberLike, compFirstNameLike, compLastNameLike, respFirstNameLike, respLastNameLike));
        }

        if (filter.filingType() != null && !filter.filingType().isBlank() && !"all".equalsIgnoreCase(filter.filingType())) {
            try {
                String typeStr = filter.filingType().toUpperCase();
                if (typeStr.contains("RECORD")) {
                    typeStr = "FOR_RECORD_ONLY";
                }
                FilingType filingEnum = FilingType.valueOf(typeStr);
                predicates.add(cb.equal(root.get("filingType"), filingEnum));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return predicates;
    }

    private List<Predicate> buildRecentDeletePredicates(BlotterArchiveFilterRequest filter, CriteriaBuilder cb, Root<Case> root) {
        List<Predicate> predicates = new ArrayList<>();

        // Base criteria for Recent Delete Table: isDeleted = true, NOT permanently deleted & root cases only
        predicates.add(cb.equal(root.get("isDeleted"), true));
        predicates.add(cb.or(cb.isNull(root.get("isPermanentDeleted")), cb.equal(root.get("isPermanentDeleted"), false)));
        predicates.add(cb.or(cb.isNull(root.get("previousCaseNumber")), cb.equal(root.get("previousCaseNumber"), "")));

        if (filter == null) {
            return predicates;
        }

        if (filter.search() != null && !filter.search().isBlank()) {
            String searchPattern = "%" + filter.search().trim().toLowerCase() + "%";
            Join<Case, Complainant> compJoin = root.join("complainant", JoinType.LEFT);
            Join<Complainant, Person> compPersonJoin = compJoin.join("person", JoinType.LEFT);
            Join<Case, Respondent> respJoin = root.join("respondent", JoinType.LEFT);
            Join<Respondent, Person> respPersonJoin = respJoin.join("person", JoinType.LEFT);

            Predicate caseNumberLike = cb.like(cb.lower(root.get("caseNumber")), searchPattern);
            Predicate compFirstNameLike = cb.like(cb.lower(compPersonJoin.get("firstName")), searchPattern);
            Predicate compLastNameLike = cb.like(cb.lower(compPersonJoin.get("lastName")), searchPattern);
            Predicate respFirstNameLike = cb.like(cb.lower(respPersonJoin.get("firstName")), searchPattern);
            Predicate respLastNameLike = cb.like(cb.lower(respPersonJoin.get("lastName")), searchPattern);

            predicates.add(cb.or(caseNumberLike, compFirstNameLike, compLastNameLike, respFirstNameLike, respLastNameLike));
        }

        if (filter.filingType() != null && !filter.filingType().isBlank() && !"all".equalsIgnoreCase(filter.filingType())) {
            try {
                String typeStr = filter.filingType().toUpperCase();
                if (typeStr.contains("RECORD")) {
                    typeStr = "FOR_RECORD_ONLY";
                }
                FilingType filingEnum = FilingType.valueOf(typeStr);
                predicates.add(cb.equal(root.get("filingType"), filingEnum));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return predicates;
    }
}
