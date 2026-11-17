package com.barangay.usermanagement.user.controller;

import com.barangay.usermanagement.user.service.AdminDashboardService;
import com.barangay.zshared.dto.AdminDashboardDTO;
import com.barangay.zshared.dto.BarangayAdminDashboardKpiDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing dashboard analytics and key performance indicators (KPIs)
 * for administrators and system operations.
 */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    /**
     * Retrieves overall system performance metrics, server resources, and user growth trends.
     */
    @GetMapping("/stats")
    public ResponseEntity<AdminDashboardDTO> getDashboardStats() {
        return ResponseEntity.ok(adminDashboardService.getDashboardStats());
    }

    /**
     * Retrieves operational KPIs for the barangay, including weekly cases and issuance statistics.
     */
    @GetMapping("/kpi")
    public ResponseEntity<BarangayAdminDashboardKpiDTO> getDashboardKpiStats() {
        return ResponseEntity.ok(adminDashboardService.getDashboardKpiStats());
    }
}
