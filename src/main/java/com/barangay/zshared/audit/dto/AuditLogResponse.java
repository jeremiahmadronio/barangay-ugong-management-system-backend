package com.barangay.zshared.audit.dto;

import com.barangay.zshared.enumerated.Severity;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        LocalDateTime timestamp,
        String userName,
        String userRole,
        String action,
        String module,
        String ipAddress,
        Severity severity,
        String details,
        String reason,
        String oldValue,
        String newValue
) {
}
