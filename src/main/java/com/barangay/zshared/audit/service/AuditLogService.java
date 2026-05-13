package com.barangay.zshared.audit.service;

import com.barangay.user_management.user.model.User;
import com.barangay.zshared.audit.model.AuditLogs;
import com.barangay.zshared.audit.repository.AuditLogsRepository;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Handles saving audit log records to the database.
 * Runs asynchronously so it does not slow down the main request.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogsRepository auditLogsRepository;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest httpRequest;

    /**
     * Saves an audit log entry.
     * Runs in a separate transaction so even if the main transaction fails,
     * the audit log is still saved.
     */
    @Async
    public void log(
        User user,
        AuditModule module,
        String actionTaken,
        Severity severity,
        String reason,
        Object oldValue,
        Object newValue
        ){
        try{

            AuditLogs auditLog = AuditLogs.builder()
                    .user(user)
                    .module(module)
                    .actionTaken(actionTaken)
                    .severity(severity)
                    .reason(reason)
                    .ipAddress(getClientIp())
                    .oldValue(toJson(oldValue))
                    .newValue(toJson(newValue))
                    .build();
            auditLogsRepository.save(auditLog);
        }catch (Exception ex){
            log.error("Failed to save audit log: {}", ex.getMessage(), ex);

        }
    }

    // Converts any object to a JSON string for storage
    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            // Never let audit logging break the main flow
            log.warn("Could not serialize object to JSON: {}", e.getMessage());
            return obj.toString();
        }
    }


    // Gets the real IP address of the client — handles proxies
    private String getClientIp() {
        try {
            String[] headers = {
                    "X-Forwarded-For", "Proxy-Client-IP",
                    "WL-Proxy-Client-IP", "HTTP_CLIENT_IP",
                    "HTTP_X_FORWARDED_FOR"
            };
            for (String header : headers) {
                String ip = httpRequest.getHeader(header);
                if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                    return ip.contains(",") ? ip.split(",")[0].trim() : ip;
                }
            }
            return httpRequest.getRemoteAddr();
        } catch (Exception e) {
            return "0.0.0.0";
        }
    }


}
