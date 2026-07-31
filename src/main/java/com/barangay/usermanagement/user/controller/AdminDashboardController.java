package com.barangay.usermanagement.user.controller;

import com.barangay.usermanagement.user.service.AdminDashboardService;
import com.barangay.zshared.dto.AdminDashboardDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/stats")
    public ResponseEntity<AdminDashboardDTO> getDashboardStats() {
        return ResponseEntity.ok(adminDashboardService.getDashboardStats());
    }
}
