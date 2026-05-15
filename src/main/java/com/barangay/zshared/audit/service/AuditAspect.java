package com.barangay.zshared.audit.service;

import com.barangay.user_management.user.model.User;
import com.barangay.zshared.audit.annotations.LogAudit;
import com.barangay.zshared.security.CustomUserDetails;
import com.barangay.zshared.util.NetworkUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Intercepts any method annotated with @LogAudit and saves an audit log.
 * Uses Spring AOP to automatically capture action details without
 * polluting the business logic in Service classes.
 */
@Aspect
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogService auditService;

    /**
     * The main interceptor logic.
     * @Around ensures we can capture data BEFORE and AFTER the method runs.
     */
    @Around("@annotation(com.barangay.zshared.audit.annotations.LogAudit)")
    public Object intercept(ProceedingJoinPoint joinPoint) throws Throwable {
        // Get the annotation details
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        LogAudit annotation = method.getAnnotation(LogAudit.class);

        // Capture old value — first argument of the method (if enabled)
        Object oldValue = null;
        if (annotation.captureOldValue()) {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                oldValue = args[0];
            }
        }

        // Run the actual method
        Object result = joinPoint.proceed();

        // Capture new value — return value of the method (if enabled)
        Object newValue = null;
        if (annotation.captureNewValue()) {
            newValue = result;
        }

        // Get the currently logged-in user
        User currentUser = getCurrentUser();

        // Get the current ipAddress of the user
        String clientIp = NetworkUtil.getClientIp();

        // Save the audit log asynchronously
        auditService.log(
                currentUser,
                annotation.module(),
                annotation.action(),
                annotation.severity(),
                annotation.reason().isBlank() ? null : annotation.reason(),
                clientIp,
                oldValue,
                newValue
        );

        return result;
    }


    /**
     * Extracts the User entity from the Spring Security context.
     */
    private User getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return null;
            CustomUserDetails details = (CustomUserDetails) auth.getPrincipal();
            return details.user();
        } catch (Exception e) {
            log.warn("Could not get current user for audit log: {}", e.getMessage());
            return null;
        }
    }
}
