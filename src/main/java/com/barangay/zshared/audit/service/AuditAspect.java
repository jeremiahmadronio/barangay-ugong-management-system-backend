package com.barangay.zshared.audit.service;

import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.audit.annotations.LogAudit;
import com.barangay.zshared.security.CustomUserDetails;
import com.barangay.zshared.util.NetworkUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Aspect that intercepts methods annotated with {@link LogAudit} and persists audit entries.
 */
@Aspect
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogService auditService;

    /**
     * Intercepts execution of methods annotated with {@link LogAudit} to capture audit metadata.
     */
    @Around("@annotation(com.barangay.zshared.audit.annotations.LogAudit)")
    public Object intercept(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        LogAudit annotation = method.getAnnotation(LogAudit.class);

        Object oldValue = null;
        if (annotation.captureOldValue()) {
            Object[] args = joinPoint.getArgs();
            int argIdx = annotation.oldValueArgIndex();
            if (args != null && args.length > argIdx) {
                oldValue = args[argIdx];
            }
        }

        Object result = joinPoint.proceed();

        Object newValue = null;
        if (annotation.captureNewValue() && result != null) {
            boolean isVoidResponse = result instanceof ResponseEntity<?> re
                    && (re.getBody() == null);
            if (!isVoidResponse) {
                newValue = result;
            }
        }

        User currentUser = getCurrentUser();
        String clientIp = NetworkUtil.getClientIp();

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
     * Extracts the authenticated {@link User} entity from the security context.
     */
    private User getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return null;
            }
            CustomUserDetails details = (CustomUserDetails) auth.getPrincipal();
            return details.user();
        } catch (Exception e) {
            log.warn("Could not get current user for audit log: {}", e.getMessage());
            return null;
        }
    }
}
