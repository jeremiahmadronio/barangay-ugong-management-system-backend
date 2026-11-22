package com.barangay.kapitana.controller;

import com.barangay.kapitana.dto.KapitanaDashboardKpiResponse;
import com.barangay.kapitana.dto.KapitanaRecentCaseItem;
import com.barangay.kapitana.service.KapitanaDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for the Punong Barangay (Kapitana) Executive Dashboard and cross-department case feeds.
 */
@RestController
@RequestMapping("/api/v1/kapitana")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class KapitanaDashboardController {

    private final KapitanaDashboardService kapitanaDashboardService;

    /**
     * Retrieves high-level KPI metrics across all barangay departments and resident demographics.
     *
     * @return executive dashboard KPI summary
     */
    @GetMapping("/dashboard-kpi")
    public ResponseEntity<KapitanaDashboardKpiResponse> getDashboardKpi() {
        log.info("REST request to fetch Kapitana Executive Dashboard KPI stats");
        KapitanaDashboardKpiResponse response = kapitanaDashboardService.getDashboardKpi();
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the latest cases across all departments for the unified activity feed.
     *
     * @param limit maximum number of case records to return (defaults to 10)
     * @return list of recent cases across departments
     */
    @GetMapping("/recent-cases")
    public ResponseEntity<List<KapitanaRecentCaseItem>> getRecentCases(
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        log.info("REST request to fetch latest unified cases for Kapitana, limit={}", limit);
        List<KapitanaRecentCaseItem> recentCases = kapitanaDashboardService.getRecentCases(limit);
        return ResponseEntity.ok(recentCases);
    }
}
