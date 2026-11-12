package com.barangay.kapitana.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.barangay.kapitana.dto.KapitanaDashboardKpiResponse;
import com.barangay.kapitana.dto.KapitanaRecentCaseItem;
import com.barangay.kapitana.service.KapitanaDashboardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/kapitana")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class KapitanaDashboardController {

    private final KapitanaDashboardService kapitanaDashboardService;

    @GetMapping("/dashboard-kpi")
    public ResponseEntity<KapitanaDashboardKpiResponse> getDashboardKpi() {
        log.info("REST request to fetch Kapitana Executive Dashboard KPI stats");
        KapitanaDashboardKpiResponse response = kapitanaDashboardService.getDashboardKpi();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent-cases")
    public ResponseEntity<List<KapitanaRecentCaseItem>> getRecentCases(
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        log.info("REST request to fetch latest unified cases for Kapitana, limit={}", limit);
        List<KapitanaRecentCaseItem> recentCases = kapitanaDashboardService.getRecentCases(limit);
        return ResponseEntity.ok(recentCases);
    }
}
