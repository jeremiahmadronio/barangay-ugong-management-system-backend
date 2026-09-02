package com.barangay.clearance.controller;

import com.barangay.clearance.dto.ClearanceDailyTrendResponse;
import com.barangay.clearance.dto.ClearanceMonthlyTrendResponse;
import com.barangay.clearance.dto.ClearanceReportKpiResponse;
import com.barangay.clearance.service.ClearanceReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/clearance/reports")
@RequiredArgsConstructor
public class ClearanceReportController {

    private final ClearanceReportService clearanceReportService;

    /**
     * Get aggregate KPI report statistics for Clearances in date range.
     */
    @GetMapping("/kpi")
    public ResponseEntity<ClearanceReportKpiResponse> getReportKpis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long templateId
    ) {
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusMonths(6);

        return ResponseEntity.ok(clearanceReportService.getReportKpis(start, end, templateId));
    }

    /**
     * Get daily issuance trend report for Clearances in date range.
     */
    @GetMapping("/trend/daily")
    public ResponseEntity<List<ClearanceDailyTrendResponse>> getDailyTrendReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long templateId
    ) {
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays(30);

        return ResponseEntity.ok(clearanceReportService.getDailyTrendReport(start, end, templateId));
    }

    /**
     * Get monthly issuance trend report for Clearances in date range.
     */
    @GetMapping("/trend/monthly")
    public ResponseEntity<List<ClearanceMonthlyTrendResponse>> getMonthlyTrendReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long templateId
    ) {
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusMonths(6);

        return ResponseEntity.ok(clearanceReportService.getMonthlyTrendReport(start, end, templateId));
    }
}
