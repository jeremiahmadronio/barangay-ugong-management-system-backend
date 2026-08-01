package com.barangay.cases.sharedmodel.repository;

import com.barangay.cases.sharedmodel.Case;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.FilingType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CaseRepositoryCustomImpl implements CaseRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Case> findFilteredCases(String search, String status, String natureOfComplaint, String filingType, Pageable pageable) {
        StringBuilder whereClause = new StringBuilder(" WHERE (c.isArchived = false OR c.isArchived IS NULL) ");
        Map<String, Object> params = new HashMap<>();

        if (filingType != null && !filingType.isBlank() && !filingType.equalsIgnoreCase("all")) {
            if (filingType.trim().toUpperCase().contains("FORMAL")) {
                whereClause.append(" AND c.filingType = :filingType ");
                params.put("filingType", FilingType.FORMAL_COMPLAINT);
            } else if (filingType.trim().toUpperCase().contains("RECORD")) {
                whereClause.append(" AND (c.filingType = :filingType OR c.filingType IS NULL) ");
                params.put("filingType", FilingType.FOR_RECORD_ONLY);
            }
        }

        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("all")) {
            try {
                CaseStatus cs = CaseStatus.valueOf(status.trim().toUpperCase().replace(" ", "_"));
                whereClause.append(" AND c.caseStatus = :caseStatus ");
                params.put("caseStatus", cs);
            } catch (Exception ignored) {
            }
        }

        if (search != null && !search.isBlank()) {
            whereClause.append(" AND (LOWER(c.caseNumber) LIKE :search ")
                       .append(" OR LOWER(c.complainant.person.lastName) LIKE :search ")
                       .append(" OR LOWER(c.respondent.person.lastName) LIKE :search) ");
            params.put("search", "%" + search.trim().toLowerCase() + "%");
        }

        if (natureOfComplaint != null && !natureOfComplaint.isBlank() && !natureOfComplaint.equalsIgnoreCase("all")) {
            whereClause.append(" AND LOWER(c.incidentDetail.natureOfComplaint) LIKE :nature ");
            params.put("nature", "%" + natureOfComplaint.trim().toLowerCase() + "%");
        }

        // 1. Count query for total matching elements
        String countJpql = "SELECT COUNT(c) FROM Case c " + whereClause;
        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);
        params.forEach(countQuery::setParameter);
        long total = countQuery.getSingleResult();

        // 2. Optimized data fetch query with JOIN FETCH
        String dataJpql = "SELECT c FROM Case c LEFT JOIN FETCH c.complainant LEFT JOIN FETCH c.respondent LEFT JOIN FETCH c.incidentDetail "
                + whereClause + " ORDER BY c.id DESC";
        TypedQuery<Case> dataQuery = entityManager.createQuery(dataJpql, Case.class);
        params.forEach(dataQuery::setParameter);

        if (pageable != null && pageable.isPaged()) {
            dataQuery.setFirstResult((int) pageable.getOffset());
            dataQuery.setMaxResults(pageable.getPageSize());
        }

        List<Case> content = dataQuery.getResultList();
        return new PageImpl<>(content, pageable != null ? pageable : Pageable.unpaged(), total);
    }

    @Override
    public List<Case> findFilteredCasesList(String search, String status, String natureOfComplaint, String filingType) {
        return findFilteredCases(search, status, natureOfComplaint, filingType, Pageable.unpaged()).getContent();
    }
}
