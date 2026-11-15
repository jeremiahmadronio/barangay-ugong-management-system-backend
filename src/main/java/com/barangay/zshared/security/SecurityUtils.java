package com.barangay.zshared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.barangay.usermanagement.user.model.User;

import lombok.extern.slf4j.Slf4j;

/**
 * Shared security utility for retrieving authenticated user context.
 */
@Slf4j
@Component
public class SecurityUtils {

    /**
     * Extracts the logged-in {@link User} entity from Spring SecurityContextHolder.
     *
     * @return the authenticated {@link User}, or {@code null} if unauthenticated
     */
    public static User getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof CustomUserDetails(User user)) {
                return user;
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve authenticated user from SecurityContext: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Checks if the currently authenticated user has a specific permission.
     * System admins and root admins always have full access.
     *
     * @param permissionName the permission display name to verify (e.g. "File Complaint")
     * @return true if authorized, false otherwise
     */
    public static boolean hasPermission(String permissionName) {
        User user = getCurrentUser();
        if (user == null) {
            return false;
        }
        if (user.getRole() != null && user.getRole().getRoleName() != null) {
            String roleName = user.getRole().getRoleName().trim();
            if ("ROOT_ADMIN".equalsIgnoreCase(roleName) || "SYSTEM_ADMIN".equalsIgnoreCase(roleName)) {
                return true;
            }
        }
        if (user.getCustomPermissions() == null || user.getCustomPermissions().isEmpty()) {
            return false;
        }
        String normalizedTarget = permissionName.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
        String baseTarget = normalizedTarget.replaceAll("s$", "");
        return user.getCustomPermissions().stream().anyMatch(p -> {
            if (p == null || p.getPermissionName() == null) return false;
            String norm = p.getPermissionName().trim().toLowerCase().replaceAll("[^a-z0-9]", "");
            String base = norm.replaceAll("s$", "");
            if (norm.equals(normalizedTarget) || base.equals(baseTarget)) return true;
            if (normalizedTarget.contains("note") && norm.contains("note")) return true;
            if (normalizedTarget.contains("mediation") && norm.contains("mediation")) return true;
            if (normalizedTarget.contains("conciliation") && norm.contains("conciliation")) return true;
            if ((normalizedTarget.contains("cfa") || normalizedTarget.contains("certifiedtofileaction"))
                    && (norm.contains("cfa") || norm.contains("certifiedtofileaction"))) return true;
            if (normalizedTarget.contains("reopen") && norm.contains("reopen")) return true;
            if (normalizedTarget.contains("settle") && norm.contains("settle")) return true;
            if (normalizedTarget.contains("dismiss") && norm.contains("dismiss")) return true;
            if (normalizedTarget.contains("edit") && norm.contains("edit")) return true;
            if ((normalizedTarget.contains("lupon") || normalizedTarget.contains("escalate")) && (norm.contains("lupon") || norm.contains("escalate"))) return true;
            if (normalizedTarget.contains("permanent") && norm.contains("permanent")) return true;
            if (normalizedTarget.contains("delete") && !normalizedTarget.contains("permanent")
                    && norm.contains("delete") && !norm.contains("permanent")) return true;
            if (normalizedTarget.contains("restore") && norm.contains("restore")) return true;
            if (normalizedTarget.contains("archive") && norm.contains("archive")) return true;
            if (normalizedTarget.contains("view") && norm.contains("view")) return true;
            return false;
        });
    }
}
