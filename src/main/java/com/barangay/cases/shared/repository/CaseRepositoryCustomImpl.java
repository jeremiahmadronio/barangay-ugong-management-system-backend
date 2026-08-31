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

import java.time.LocalDateTime;
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
    public Page<Case> findLuponCasesFiltered(com.barangay.cases.lupon.dto.LuponFilterRequest filter, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // 1. Data Query
        CriteriaQuery<Case> cq = cb.createQuery(Case.class);
        Root<Case> root = cq.from(Case.class);

        List<Predicate> predicates = buildLuponPredicates(filter, cb, cq, root);
        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(root.get("createdAt")));

        TypedQuery<Case> query = entityManager.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Case> resultList = query.getResultList();

        // 2. Count Query
        CriteriaQuery<Long> countCq = cb.createQuery(Long.class);
        Root<Case> countRoot = countCq.from(Case.class);

        List<Predicate> countPredicates = buildLuponPredicates(filter, cb, countCq, countRoot);
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
        List<BlotterArchiveItemResponse> content = cases.stream().map(rootCase -> {
            Case latestCase = resolveLatestChildCase(rootCase);

            String compName = "—";
            Complainant comp = latestCase.getComplainant() != null ? latestCase.getComplainant() : rootCase.getComplainant();
            if (comp != null && comp.getPerson() != null) {
                Person p = comp.getPerson();
                String f = p.getFirstName() != null ? p.getFirstName() : "";
                String l = p.getLastName() != null ? p.getLastName() : "";
                String m = p.getMiddleName() != null ? p.getMiddleName() : "";
                compName = (l + (l.isBlank() || f.isBlank() ? "" : ", ") + f + (m.isBlank() ? "" : " " + m.charAt(0) + ".")).trim();
            }

            String respName = "—";
            Respondent resp = latestCase.getRespondent() != null ? latestCase.getRespondent() : rootCase.getRespondent();
            if (resp != null && resp.getPerson() != null) {
                Person p = resp.getPerson();
                String f = p.getFirstName() != null ? p.getFirstName() : "";
                String l = p.getLastName() != null ? p.getLastName() : "";
                String m = p.getMiddleName() != null ? p.getMiddleName() : "";
                respName = (l + (l.isBlank() || f.isBlank() ? "" : ", ") + f + (m.isBlank() ? "" : " " + m.charAt(0) + ".")).trim();
            }

            String remarks = rootCase.getArchiveRemarks() != null ? rootCase.getArchiveRemarks() : (latestCase.getArchiveRemarks() != null ? latestCase.getArchiveRemarks() : "Archived Record");
            LocalDateTime archivedAt = rootCase.getArchivedAt() != null ? rootCase.getArchivedAt() : (latestCase.getArchivedAt() != null ? latestCase.getArchivedAt() : rootCase.getUpdatedAt());

            return new BlotterArchiveItemResponse(
                    rootCase.getId(),
                    rootCase.getCaseNumber(),
                    compName,
                    respName,
                    remarks,
                    latestCase.getCaseStatus(),
                    archivedAt,
                    rootCase.getFilingType()
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
        List<BlotterRecentDeleteItemResponse> content = cases.stream().map(rootCase -> {
            Case latestCase = resolveLatestChildCase(rootCase);

            String compName = "—";
            Complainant comp = latestCase.getComplainant() != null ? latestCase.getComplainant() : rootCase.getComplainant();
            if (comp != null && comp.getPerson() != null) {
                Person p = comp.getPerson();
                String f = p.getFirstName() != null ? p.getFirstName() : "";
                String l = p.getLastName() != null ? p.getLastName() : "";
                String m = p.getMiddleName() != null ? p.getMiddleName() : "";
                compName = (l + (l.isBlank() || f.isBlank() ? "" : ", ") + f + (m.isBlank() ? "" : " " + m.charAt(0) + ".")).trim();
            }

            String respName = "—";
            Respondent resp = latestCase.getRespondent() != null ? latestCase.getRespondent() : rootCase.getRespondent();
            if (resp != null && resp.getPerson() != null) {
                Person p = resp.getPerson();
                String f = p.getFirstName() != null ? p.getFirstName() : "";
                String l = p.getLastName() != null ? p.getLastName() : "";
                String m = p.getMiddleName() != null ? p.getMiddleName() : "";
                respName = (l + (l.isBlank() || f.isBlank() ? "" : ", ") + f + (m.isBlank() ? "" : " " + m.charAt(0) + ".")).trim();
            }

            LocalDateTime deletedAt = rootCase.getDeletedAt() != null ? rootCase.getDeletedAt() : (latestCase.getDeletedAt() != null ? latestCase.getDeletedAt() : rootCase.getUpdatedAt());

            return new BlotterRecentDeleteItemResponse(
                    rootCase.getId(),
                    rootCase.getCaseNumber(),
                    compName,
                    respName,
                    latestCase.getCaseStatus(),
                    deletedAt,
                    rootCase.getFilingType()
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

    private Case resolveLatestChildCase(Case rootCase) {
        if (rootCase == null) return null;
        Case curr = rootCase;
        while (curr.getReOpenedCaseNumber() != null && !curr.getReOpenedCaseNumber().trim().isEmpty()) {
            TypedQuery<Case> tq = entityManager.createQuery("SELECT c FROM Case c WHERE c.caseNumber = :cn", Case.class);
            tq.setParameter("cn", curr.getReOpenedCaseNumber().trim());
            List<Case> res = tq.getResultList();
            if (!res.isEmpty()) {
                curr = res.get(0);
            } else {
                break;
            }
        }
        return curr;
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

    private List<Predicate> buildLuponPredicates(
            com.barangay.cases.lupon.dto.LuponFilterRequest filter,
            CriteriaBuilder cb,
            CommonAbstractCriteria query,
            Root<Case> root
    ) {
        List<Predicate> predicates = new ArrayList<>();

        // Base criteria for Lupon:
        // 1. Root cases only
        predicates.add(cb.or(cb.isNull(root.get("previousCaseNumber")), cb.equal(root.get("previousCaseNumber"), "")));

        // 2. Non-deleted, non-permanently-deleted, non-archived
        predicates.add(cb.or(cb.isNull(root.get("isDeleted")), cb.equal(root.get("isDeleted"), false)));
        predicates.add(cb.or(cb.isNull(root.get("isPermanentDeleted")), cb.equal(root.get("isPermanentDeleted"), false)));
        predicates.add(cb.or(cb.isNull(root.get("isArchived")), cb.equal(root.get("isArchived"), false)));

        // 3. Exclude For The Record Only (Lupon handles formal complaints only)
        predicates.add(cb.or(cb.isNull(root.get("filingType")), cb.equal(root.get("filingType"), FilingType.FORMAL_COMPLAINT)));

        // 4. MUST be escalated / referred to Lupon (root OR any reopened child in its lineage)
        Predicate hasLuponReferral = cb.isNotNull(root.get("luponReferral"));
        Predicate isUnderConciliation = cb.equal(root.get("caseStatus"), CaseStatus.UNDER_CONCILIATION);
        Predicate isLuponDept = cb.and(
                cb.isNotNull(root.get("department")),
                cb.equal(cb.upper(root.get("department").get("name")), "LUPON")
        );

        Subquery<Long> childLuponSq = query.subquery(Long.class);
        Root<Case> childRoot = childLuponSq.from(Case.class);
        childLuponSq.select(cb.literal(1L));
        childLuponSq.where(
                cb.equal(childRoot.get("previousCaseNumber"), root.get("caseNumber")),
                cb.or(
                        cb.isNotNull(childRoot.get("luponReferral")),
                        cb.equal(childRoot.get("caseStatus"), CaseStatus.UNDER_CONCILIATION),
                        cb.and(
                                cb.isNotNull(childRoot.get("department")),
                                cb.equal(cb.upper(childRoot.get("department").get("name")), "LUPON")
                        )
                )
        );

        predicates.add(cb.or(hasLuponReferral, isUnderConciliation, isLuponDept, cb.exists(childLuponSq)));

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

            Subquery<Long> childSearchSq = query.subquery(Long.class);
            Root<Case> childSearchRoot = childSearchSq.from(Case.class);
            childSearchSq.select(cb.literal(1L));
            childSearchSq.where(
                    cb.equal(childSearchRoot.get("previousCaseNumber"), root.get("caseNumber")),
                    cb.like(cb.lower(childSearchRoot.get("caseNumber")), searchPattern)
            );

            predicates.add(cb.or(caseNumberLike, compFirstNameLike, compLastNameLike, respFirstNameLike, respLastNameLike, cb.exists(childSearchSq)));
        }

        // Status Filter
        if (filter.status() != null && !filter.status().isBlank() && !"all".equalsIgnoreCase(filter.status())) {
            try {
                CaseStatus statusEnum = CaseStatus.valueOf(filter.status().toUpperCase());
                Predicate rootStatusMatch = cb.equal(root.get("caseStatus"), statusEnum);

                Subquery<Long> childStatusSq = query.subquery(Long.class);
                Root<Case> childStatusRoot = childStatusSq.from(Case.class);
                childStatusSq.select(cb.literal(1L));
                childStatusSq.where(
                        cb.equal(childStatusRoot.get("previousCaseNumber"), root.get("caseNumber")),
                        cb.equal(childStatusRoot.get("caseStatus"), statusEnum)
                );

                predicates.add(cb.or(rootStatusMatch, cb.exists(childStatusSq)));
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

    @Override
    public Page<BlotterArchiveItemResponse> findArchivedLuponCases(BlotterArchiveFilterRequest filter, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // 1. Data Query
        CriteriaQuery<Case> cq = cb.createQuery(Case.class);
        Root<Case> root = cq.from(Case.class);

        List<Predicate> predicates = buildLuponArchivePredicates(filter, cb, cq, root);
        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(cb.coalesce(root.get("archivedAt"), root.get("updatedAt"))));

        TypedQuery<Case> query = entityManager.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Case> cases = query.getResultList();
        List<BlotterArchiveItemResponse> content = cases.stream().map(rootCase -> {
            Case latestCase = resolveLatestChildCase(rootCase);

            String compName = "—";
            Complainant comp = latestCase.getComplainant() != null ? latestCase.getComplainant() : rootCase.getComplainant();
            if (comp != null && comp.getPerson() != null) {
                Person p = comp.getPerson();
                String f = p.getFirstName() != null ? p.getFirstName() : "";
                String l = p.getLastName() != null ? p.getLastName() : "";
                String m = p.getMiddleName() != null ? p.getMiddleName() : "";
                compName = (l + (l.isBlank() || f.isBlank() ? "" : ", ") + f + (m.isBlank() ? "" : " " + m.charAt(0) + ".")).trim();
            }

            String respName = "—";
            Respondent resp = latestCase.getRespondent() != null ? latestCase.getRespondent() : rootCase.getRespondent();
            if (resp != null && resp.getPerson() != null) {
                Person p = resp.getPerson();
                String f = p.getFirstName() != null ? p.getFirstName() : "";
                String l = p.getLastName() != null ? p.getLastName() : "";
                String m = p.getMiddleName() != null ? p.getMiddleName() : "";
                respName = (l + (l.isBlank() || f.isBlank() ? "" : ", ") + f + (m.isBlank() ? "" : " " + m.charAt(0) + ".")).trim();
            }

            String remarks = rootCase.getArchiveRemarks() != null ? rootCase.getArchiveRemarks() : (latestCase.getArchiveRemarks() != null ? latestCase.getArchiveRemarks() : "Archived Record");
            LocalDateTime archivedAt = rootCase.getArchivedAt() != null ? rootCase.getArchivedAt() : (latestCase.getArchivedAt() != null ? latestCase.getArchivedAt() : rootCase.getUpdatedAt());

            return new BlotterArchiveItemResponse(
                    rootCase.getId(),
                    rootCase.getCaseNumber(),
                    compName,
                    respName,
                    remarks,
                    latestCase.getCaseStatus(),
                    archivedAt,
                    rootCase.getFilingType()
            );
        }).toList();

        // 2. Count Query
        CriteriaQuery<Long> countCq = cb.createQuery(Long.class);
        Root<Case> countRoot = countCq.from(Case.class);
        List<Predicate> countPredicates = buildLuponArchivePredicates(filter, cb, countCq, countRoot);
        countCq.select(cb.count(countRoot));
        countCq.where(countPredicates.toArray(new Predicate[0]));

        Long totalCount = entityManager.createQuery(countCq).getSingleResult();

        return new PageImpl<>(content, pageable, totalCount);
    }

    @Override
    public Page<BlotterRecentDeleteItemResponse> findRecentDeletedLuponCases(BlotterArchiveFilterRequest filter, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // 1. Data Query
        CriteriaQuery<Case> cq = cb.createQuery(Case.class);
        Root<Case> root = cq.from(Case.class);

        List<Predicate> predicates = buildLuponRecentDeletePredicates(filter, cb, cq, root);
        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(cb.coalesce(root.get("deletedAt"), root.get("updatedAt"))));

        TypedQuery<Case> query = entityManager.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Case> cases = query.getResultList();
        List<BlotterRecentDeleteItemResponse> content = cases.stream().map(rootCase -> {
            Case latestCase = resolveLatestChildCase(rootCase);

            String compName = "—";
            Complainant comp = latestCase.getComplainant() != null ? latestCase.getComplainant() : rootCase.getComplainant();
            if (comp != null && comp.getPerson() != null) {
                Person p = comp.getPerson();
                String f = p.getFirstName() != null ? p.getFirstName() : "";
                String l = p.getLastName() != null ? p.getLastName() : "";
                String m = p.getMiddleName() != null ? p.getMiddleName() : "";
                compName = (l + (l.isBlank() || f.isBlank() ? "" : ", ") + f + (m.isBlank() ? "" : " " + m.charAt(0) + ".")).trim();
            }

            String respName = "—";
            Respondent resp = latestCase.getRespondent() != null ? latestCase.getRespondent() : rootCase.getRespondent();
            if (resp != null && resp.getPerson() != null) {
                Person p = resp.getPerson();
                String f = p.getFirstName() != null ? p.getFirstName() : "";
                String l = p.getLastName() != null ? p.getLastName() : "";
                String m = p.getMiddleName() != null ? p.getMiddleName() : "";
                respName = (l + (l.isBlank() || f.isBlank() ? "" : ", ") + f + (m.isBlank() ? "" : " " + m.charAt(0) + ".")).trim();
            }

            LocalDateTime deletedAt = rootCase.getDeletedAt() != null ? rootCase.getDeletedAt() : (latestCase.getDeletedAt() != null ? latestCase.getDeletedAt() : rootCase.getUpdatedAt());

            return new BlotterRecentDeleteItemResponse(
                    rootCase.getId(),
                    rootCase.getCaseNumber(),
                    compName,
                    respName,
                    latestCase.getCaseStatus(),
                    deletedAt,
                    rootCase.getFilingType()
            );
        }).toList();

        // 2. Count Query
        CriteriaQuery<Long> countCq = cb.createQuery(Long.class);
        Root<Case> countRoot = countCq.from(Case.class);
        List<Predicate> countPredicates = buildLuponRecentDeletePredicates(filter, cb, countCq, countRoot);
        countCq.select(cb.count(countRoot));
        countCq.where(countPredicates.toArray(new Predicate[0]));

        Long totalCount = entityManager.createQuery(countCq).getSingleResult();

        return new PageImpl<>(content, pageable, totalCount);
    }

    private List<Predicate> buildLuponArchivePredicates(
            BlotterArchiveFilterRequest filter,
            CriteriaBuilder cb,
            CommonAbstractCriteria query,
            Root<Case> root
    ) {
        List<Predicate> predicates = new ArrayList<>();

        // Base criteria for Archive Table: isArchived = true, NOT soft-deleted, NOT permanently deleted & root cases only
        predicates.add(cb.equal(root.get("isArchived"), true));
        predicates.add(cb.or(cb.isNull(root.get("isDeleted")), cb.equal(root.get("isDeleted"), false)));
        predicates.add(cb.or(cb.isNull(root.get("isPermanentDeleted")), cb.equal(root.get("isPermanentDeleted"), false)));
        predicates.add(cb.or(cb.isNull(root.get("previousCaseNumber")), cb.equal(root.get("previousCaseNumber"), "")));

        // Lupon Department Only Scope
        Subquery<Integer> referralSubquery = query.subquery(Integer.class);
        Root<Case> subRoot = referralSubquery.from(Case.class);
        referralSubquery.select(cb.literal(1));

        Predicate linkMatches = cb.equal(subRoot.get("previousCaseNumber"), root.get("caseNumber"));
        Predicate subHasReferral = cb.isNotNull(subRoot.get("luponReferral"));
        Predicate subIsConciliation = cb.equal(subRoot.get("caseStatus"), CaseStatus.UNDER_CONCILIATION);
        Predicate subIsLuponDept = cb.equal(cb.upper(subRoot.get("department").get("name")), "LUPON");

        referralSubquery.where(cb.and(linkMatches, cb.or(subHasReferral, subIsConciliation, subIsLuponDept)));

        Predicate rootHasReferral = cb.isNotNull(root.get("luponReferral"));
        Predicate rootIsConciliation = cb.equal(root.get("caseStatus"), CaseStatus.UNDER_CONCILIATION);
        Predicate rootIsLuponDept = cb.equal(cb.upper(root.get("department").get("name")), "LUPON");

        predicates.add(cb.or(rootHasReferral, rootIsConciliation, rootIsLuponDept, cb.exists(referralSubquery)));

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

    private List<Predicate> buildLuponRecentDeletePredicates(
            BlotterArchiveFilterRequest filter,
            CriteriaBuilder cb,
            CommonAbstractCriteria query,
            Root<Case> root
    ) {
        List<Predicate> predicates = new ArrayList<>();

        // Base criteria for Recent Delete Table: isDeleted = true, NOT permanently deleted & root cases only
        predicates.add(cb.equal(root.get("isDeleted"), true));
        predicates.add(cb.or(cb.isNull(root.get("isPermanentDeleted")), cb.equal(root.get("isPermanentDeleted"), false)));
        predicates.add(cb.or(cb.isNull(root.get("previousCaseNumber")), cb.equal(root.get("previousCaseNumber"), "")));

        // Lupon Department Only Scope
        Subquery<Integer> referralSubquery = query.subquery(Integer.class);
        Root<Case> subRoot = referralSubquery.from(Case.class);
        referralSubquery.select(cb.literal(1));

        Predicate linkMatches = cb.equal(subRoot.get("previousCaseNumber"), root.get("caseNumber"));
        Predicate subHasReferral = cb.isNotNull(subRoot.get("luponReferral"));
        Predicate subIsConciliation = cb.equal(subRoot.get("caseStatus"), CaseStatus.UNDER_CONCILIATION);
        Predicate subIsLuponDept = cb.equal(cb.upper(subRoot.get("department").get("name")), "LUPON");

        referralSubquery.where(cb.and(linkMatches, cb.or(subHasReferral, subIsConciliation, subIsLuponDept)));

        Predicate rootHasReferral = cb.isNotNull(root.get("luponReferral"));
        Predicate rootIsConciliation = cb.equal(root.get("caseStatus"), CaseStatus.UNDER_CONCILIATION);
        Predicate rootIsLuponDept = cb.equal(cb.upper(root.get("department").get("name")), "LUPON");

        predicates.add(cb.or(rootHasReferral, rootIsConciliation, rootIsLuponDept, cb.exists(referralSubquery)));

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
