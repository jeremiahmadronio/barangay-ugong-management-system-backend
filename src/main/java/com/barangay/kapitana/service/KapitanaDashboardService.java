package com.barangay.kapitana.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barangay.cases.shared.model.Case;
import com.barangay.clearance.repository.IssuedCertificateRepository;
import com.barangay.ftjs.repository.FirstTimeJobSeekerRepository;
import com.barangay.kapitana.dto.KapitanaDashboardKpiResponse;
import com.barangay.kapitana.dto.KapitanaDemographicsResponse;
import com.barangay.kapitana.dto.KapitanaDepartmentMetrics;
import com.barangay.kapitana.dto.KapitanaDepartmentOverviewResponse;
import com.barangay.kapitana.dto.KapitanaRecentCaseItem;
import com.barangay.kapitana.repository.KapitanaDashboardRepository;
import com.barangay.people.person.model.Person;
import com.barangay.people.resident.repository.ResidentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class KapitanaDashboardService {

    private final KapitanaDashboardRepository kapitanaDashboardRepository;
    private final IssuedCertificateRepository issuedCertificateRepository;
    private final FirstTimeJobSeekerRepository firstTimeJobSeekerRepository;
    private final ResidentRepository residentRepository;

    public KapitanaDashboardKpiResponse getDashboardKpi() {
        // 1. Department Breakdown (strictly excluding soft-deleted and permanently-deleted records)
        long blotterActive = kapitanaDashboardRepository.countActiveBlotterCases();
        long blotterSettled = kapitanaDashboardRepository.countSettledBlotterCases();
        long blotterTotal = kapitanaDashboardRepository.countTotalBlotterCases();
        double blotterRate = blotterTotal > 0 ? (double) blotterSettled / blotterTotal * 100.0 : 0.0;

        long luponActive = kapitanaDashboardRepository.countActiveLuponCases();
        long luponSettled = kapitanaDashboardRepository.countSettledLuponCases();
        long luponTotal = kapitanaDashboardRepository.countTotalLuponCases();
        double luponRate = luponTotal > 0 ? (double) luponSettled / luponTotal * 100.0 : 0.0;

        long vawcActive = kapitanaDashboardRepository.countActiveVawcCases();
        long vawcSettled = kapitanaDashboardRepository.countSettledVawcCases();
        long vawcTotal = kapitanaDashboardRepository.countTotalVawcCases();
        double vawcRate = vawcTotal > 0 ? (double) vawcSettled / vawcTotal * 100.0 : 0.0;

        long bcpcActive = kapitanaDashboardRepository.countActiveBcpcCases();
        long bcpcSettled = kapitanaDashboardRepository.countSettledBcpcCases();
        long bcpcTotal = kapitanaDashboardRepository.countTotalBcpcCases();
        double bcpcRate = bcpcTotal > 0 ? (double) bcpcSettled / bcpcTotal * 100.0 : 0.0;

        // 2. Services Issued (Clearance & FTJS)
        long clearanceIssued = 0;
        try {
            clearanceIssued = issuedCertificateRepository.countActiveCertificates();
        } catch (Exception e) {
            log.warn("Could not retrieve active clearance count: {}", e.getMessage());
        }

        long ftjsIssued = 0;
        try {
            ftjsIssued = firstTimeJobSeekerRepository.countTotalIssued();
        } catch (Exception e) {
            log.warn("Could not retrieve FTJS issued count: {}", e.getMessage());
        }

        // 3. Totals
        long totalActive = kapitanaDashboardRepository.countTotalActiveCases();
        long totalSettled = kapitanaDashboardRepository.countTotalSettledCases();
        long totalNonDeleted = kapitanaDashboardRepository.countTotalNonDeletedCases();
        double overallResolutionRate = totalNonDeleted > 0 ? (double) totalSettled / totalNonDeleted * 100.0 : 0.0;

        KapitanaDepartmentOverviewResponse departmentOverview = KapitanaDepartmentOverviewResponse.builder()
                .blotter(KapitanaDepartmentMetrics.builder()
                        .department("Blotter")
                        .activeCases(blotterActive)
                        .settledCases(blotterSettled)
                        .totalCases(blotterTotal)
                        .resolutionRate(Math.round(blotterRate * 10.0) / 10.0)
                        .build())
                .lupon(KapitanaDepartmentMetrics.builder()
                        .department("Lupon")
                        .activeCases(luponActive)
                        .settledCases(luponSettled)
                        .totalCases(luponTotal)
                        .resolutionRate(Math.round(luponRate * 10.0) / 10.0)
                        .build())
                .vawc(KapitanaDepartmentMetrics.builder()
                        .department("VAWC")
                        .activeCases(vawcActive)
                        .settledCases(vawcSettled)
                        .totalCases(vawcTotal)
                        .resolutionRate(Math.round(vawcRate * 10.0) / 10.0)
                        .build())
                .bcpc(KapitanaDepartmentMetrics.builder()
                        .department("BCPC")
                        .activeCases(bcpcActive)
                        .settledCases(bcpcSettled)
                        .totalCases(bcpcTotal)
                        .resolutionRate(Math.round(bcpcRate * 10.0) / 10.0)
                        .build())
                .clearanceTotalIssued(clearanceIssued)
                .ftjsTotalIssued(ftjsIssued)
                .build();

        // 4. Resident Demographics (Strictly active, non-deleted, non-permanently-deleted)
        long totalResidents = 0;
        long headsOfFamily = 0;
        long seniorCitizens = 0;
        long minorsCount = 0;
        long maleCount = 0;
        long femaleCount = 0;
        try {
            totalResidents = residentRepository.countTotalResidents();
            headsOfFamily = residentRepository.countActiveHeadsOfFamily();
            seniorCitizens = residentRepository.countActiveSeniorCitizens(LocalDate.now().minusYears(60));
            minorsCount = residentRepository.countActiveMinors(LocalDate.now().minusYears(18));
            maleCount = residentRepository.countActiveByGenderPrefix("M");
            femaleCount = residentRepository.countActiveByGenderPrefix("F");
        } catch (Exception e) {
            log.warn("Could not retrieve resident demographics: {}", e.getMessage());
        }
        long adultsCount = Math.max(0, totalResidents - seniorCitizens - minorsCount);

        KapitanaDemographicsResponse demographics = KapitanaDemographicsResponse.builder()
                .totalResidents(totalResidents)
                .headsOfFamily(headsOfFamily)
                .seniorCitizens(seniorCitizens)
                .minorsCount(minorsCount)
                .adultsCount(adultsCount)
                .maleCount(maleCount)
                .femaleCount(femaleCount)
                .build();

        return KapitanaDashboardKpiResponse.builder()
                .totalActiveCases(totalActive)
                .totalSettledCases(totalSettled)
                .totalCasesAcrossDepartments(totalNonDeleted)
                .overallResolutionRate(Math.round(overallResolutionRate * 10.0) / 10.0)
                .totalClearancesIssued(clearanceIssued)
                .totalFtjsIssued(ftjsIssued)
                .departmentOverview(departmentOverview)
                .demographics(demographics)
                .build();
    }

    public List<KapitanaRecentCaseItem> getRecentCases(int limit) {
        int fetchLimit = limit > 0 && limit <= 50 ? limit : 10;
        List<Case> cases = kapitanaDashboardRepository.findRecentCasesAcrossDepartments(PageRequest.of(0, fetchLimit));
        if (cases == null || cases.isEmpty()) {
            return Collections.emptyList();
        }

        return cases.stream().map(this::mapToRecentCaseItem).collect(Collectors.toList());
    }

    private KapitanaRecentCaseItem mapToRecentCaseItem(Case c) {
        String dept = resolveDepartmentName(c);
        String incidentNature = "Incident Record";
        if (c.getIncidentDetail() != null && c.getIncidentDetail().getNatureOfComplaint() != null) {
            incidentNature = c.getIncidentDetail().getNatureOfComplaint();
        }

        String complainant = "Anonymous / Not Stated";
        if (c.getComplainant() != null && c.getComplainant().getPerson() != null) {
            complainant = formatPersonName(c.getComplainant().getPerson());
        }

        String respondent = "Unidentified / Not Stated";
        if (c.getRespondent() != null && c.getRespondent().getPerson() != null) {
            respondent = formatPersonName(c.getRespondent().getPerson());
        }

        return KapitanaRecentCaseItem.builder()
                .id(c.getId())
                .caseNumber(c.getCaseNumber())
                .department(dept)
                .incidentNature(incidentNature)
                .complainantName(complainant)
                .respondentName(respondent)
                .caseStatus(c.getCaseStatus() != null ? c.getCaseStatus().name() : "PENDING")
                .filingType(c.getFilingType() != null ? c.getFilingType().name() : "FORMAL_COMPLAINT")
                .dateFiled(c.getCreatedAt())
                .build();
    }

    private String resolveDepartmentName(Case c) {
        if (c.getDepartment() != null && c.getDepartment().getName() != null) {
            String name = c.getDepartment().getName().trim().toUpperCase();
            if (name.contains("BLOTTER")) return "BLOTTER";
            if (name.contains("LUPON")) return "LUPON";
            if (name.contains("VAWC")) return "VAWC";
            if (name.contains("BCPC")) return "BCPC";
        }
        if (c.getCaseNumber() != null) {
            String upper = c.getCaseNumber().toUpperCase();
            if (upper.contains("BCPC")) return "BCPC";
            if (upper.contains("VAWC")) return "VAWC";
            if (upper.contains("LUPON")) return "LUPON";
            if (upper.contains("BLOTTER")) return "BLOTTER";
        }
        if (c.getLuponReferral() != null) {
            return "LUPON";
        }
        return "BLOTTER";
    }

    private String formatPersonName(Person p) {
        String first = p.getFirstName() != null ? p.getFirstName().trim() : "";
        String last = p.getLastName() != null ? p.getLastName().trim() : "";
        if (first.isEmpty() && last.isEmpty()) return "Confidential";
        if (first.isEmpty()) return last;
        if (last.isEmpty()) return first;
        return last + ", " + first;
    }
}
