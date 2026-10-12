package com.barangay.usermanagement.user.controller;

import com.barangay.usermanagement.user.service.BarangayAdminDashboardService;
import com.barangay.zshared.dto.BarangayAdminDashboardKpiDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class BarangayAdminDashboardController {

    private final BarangayAdminDashboardService dashboardService;

    @GetMapping("/kpi")
    public ResponseEntity<BarangayAdminDashboardKpiDTO> getDashboardKpiStats() {
        return ResponseEntity.ok(dashboardService.getDashboardKpiStats());
    }
}
