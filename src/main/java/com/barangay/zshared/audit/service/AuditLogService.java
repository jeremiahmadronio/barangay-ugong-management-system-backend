package com.barangay.zshared.audit.service;

import com.barangay.people.person.model.Person;
import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.audit.dto.AuditLogResponse;
import com.barangay.zshared.audit.dto.AuditLogStatsResponse;
import com.barangay.zshared.audit.model.AuditLogs;
import com.barangay.zshared.audit.repository.AuditLogsRepository;
import com.barangay.zshared.dto.PageResponse;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.barangay.zshared.audit.repository.AuditLogSpecifications.createdOnOrAfter;
import static com.barangay.zshared.audit.repository.AuditLogSpecifications.createdOnOrBefore;
import static com.barangay.zshared.audit.repository.AuditLogSpecifications.hasSeverity;
import static com.barangay.zshared.audit.repository.AuditLogSpecifications.search;

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
        String ipAddress,
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
                    .ipAddress(ipAddress)
                    .oldValue(toJson(oldValue))
                    .newValue(toJson(newValue))
                    .build();
            auditLogsRepository.save(auditLog);
        }catch (Exception ex){
            log.error("Failed to save audit log: {}", ex.getMessage(), ex);

        }
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> getAuditLogs(
            String searchTerm,
            Severity severity,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<AuditLogs> specification = Specification
                .where(search(searchTerm))
                .and(hasSeverity(severity))
                .and(createdOnOrAfter(fromDate != null ? fromDate.atStartOfDay() : null))
                .and(createdOnOrBefore(toDate != null ? toDate.atTime(LocalTime.MAX) : null));

        Page<AuditLogResponse> pageResult = auditLogsRepository.findAll(specification, pageable)
                .map(this::mapToResponse);

        return PageResponse.of(pageResult);
    }

    @Transactional(readOnly = true)
    public AuditLogStatsResponse getAuditLogStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime endOfToday = today.atTime(LocalTime.MAX);

        long totalEntries = auditLogsRepository.count();
        long todayEntries = auditLogsRepository.count(
                createdOnOrAfter(startOfToday).and(createdOnOrBefore(endOfToday))
        );
        long warningCount = auditLogsRepository.count(hasSeverity(Severity.WARNING));
        long criticalCount = auditLogsRepository.count(hasSeverity(Severity.CRITICAL));

        return new AuditLogStatsResponse(totalEntries, todayEntries, warningCount, criticalCount);
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

    private AuditLogResponse mapToResponse(AuditLogs auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getCreatedAt(),
                resolveUserName(auditLog.getUser()),
                resolveUserRole(auditLog.getUser()),
                auditLog.getActionTaken(),
                auditLog.getModule() != null ? auditLog.getModule().name() : "SYSTEM",
                auditLog.getIpAddress(),
                normalizeSeverity(auditLog.getSeverity()),
                buildDetailsPayload(auditLog),
                auditLog.getReason(),
                auditLog.getOldValue(),
                auditLog.getNewValue()
        );
    }

    private String resolveUserName(User user) {
        if (user == null) {
            return "System";
        }

        Person person = user.getPerson();
        if (person != null) {
            StringBuilder fullName = new StringBuilder();
            if (person.getFirstName() != null) {
                fullName.append(person.getFirstName().trim());
            }
            if (person.getMiddleName() != null && !person.getMiddleName().isBlank()) {
                if (fullName.length() > 0) fullName.append(' ');
                fullName.append(person.getMiddleName().trim());
            }
            if (person.getLastName() != null && !person.getLastName().isBlank()) {
                if (fullName.length() > 0) fullName.append(' ');
                fullName.append(person.getLastName().trim());
            }
            if (person.getSuffix() != null && !person.getSuffix().isBlank()) {
                if (fullName.length() > 0) fullName.append(' ');
                fullName.append(person.getSuffix().trim());
            }
            if (fullName.length() > 0) {
                return fullName.toString();
            }
        }

        return user.getUsername() != null && !user.getUsername().isBlank() ? user.getUsername() : "Unknown User";
    }

    private String resolveUserRole(User user) {
        if (user == null || user.getRole() == null || user.getRole().getRoleName() == null) {
            return "System";
        }
        return user.getRole().getRoleName();
    }

    private Severity normalizeSeverity(Severity severity) {
        if (severity == null) {
            return Severity.INFO;
        }

        return switch (severity) {
            case ERROR -> Severity.CRITICAL;
            case LOW -> Severity.INFO;
            default -> severity;
        };
    }

    private String buildDetailsPayload(AuditLogs auditLog) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reason", auditLog.getReason());
        payload.put("oldValue", tryParseJson(auditLog.getOldValue()));
        payload.put("newValue", tryParseJson(auditLog.getNewValue()));

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        } catch (Exception e) {
            return auditLog.getReason() != null ? auditLog.getReason() : "No additional details captured.";
        }
    }

    private Object tryParseJson(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readTree(rawValue);
        } catch (Exception ignored) {
            return rawValue;
        }
    }

}
