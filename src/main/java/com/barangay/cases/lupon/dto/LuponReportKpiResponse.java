package com.barangay.cases.lupon.dto;

import java.time.LocalDate;

/**
 * KPI statistics DTO response for Lupong Tagapamayapa module reports.
 *
 * @param startDate            the inclusive start date
 * @param endDate              the inclusive end date
 * @param totalReferred        total cases referred / escalated to Lupon
 * @param settledCases         total cases successfully settled at Lupon
 * @param settlementRate       percentage of settled cases out of total referred
 * @param activeConciliations  total ongoing cases under conciliation
 * @param cfaIssued            total Certificates to File Action (CFA) issued / endorsed
 */
public record LuponReportKpiResponse(
        LocalDate startDate,
        LocalDate endDate,
        long totalReferred,
        long settledCases,
        double settlementRate,
        long activeConciliations,
        long cfaIssued
) {
}
