package com.barangay.zshared.audit.controller;

import com.barangay.zshared.audit.dto.AuditLogResponse;
import com.barangay.zshared.audit.dto.AuditLogStatsResponse;
import com.barangay.zshared.audit.service.AuditLogService;
import com.barangay.zshared.enumerated.Severity;
import com.barangay.zshared.mapper.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * REST controller for retrieving system audit logs and metrics.
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * Retrieves a paginated list of system audit logs matching optional filters.
     */
    @GetMapping
    public ResponseEntity<PageResponse<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                auditLogService.getAuditLogs(search, severity, fromDate, toDate, page, size)
        );
    }

    /**
     * Retrieves overall metrics and counters for system audit logs.
     */
    @GetMapping("/stats")
    public ResponseEntity<AuditLogStatsResponse> getAuditLogStats() {
        return ResponseEntity.ok(auditLogService.getAuditLogStats());
    }
}
