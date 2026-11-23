package com.barangay.zshared.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Provides security checks that can be used directly in controller methods
 * via the @PreAuthorize annotation.
 * Registered as a Spring bean named "deptSec" so it can be used in
 * security expressions like: @PreAuthorize("@deptSec.canAccessDept(...)")
 */
@Service("deptSec")
@RequiredArgsConstructor
public class DepartmentSecurityService {

    /** Checks if the current user belongs to a specific department. */
    public boolean isInDept(Authentication auth, String deptName) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority()
                        .equals("DEPT_" + deptName.toUpperCase()));
    }

    /** Checks if the current user is the owner of the resource being accessed. */
    public boolean isOwner(Authentication auth, Long resourceOwnerId) {
        CustomUserDetails details = (CustomUserDetails) auth.getPrincipal();
        return details.user().getId().equals(resourceOwnerId);
    }

    /** Returns true if the user is ROOT_ADMIN, SYSTEM_ADMIN, or belongs to the given department. */
    public boolean canAccessDept(Authentication auth, String deptName) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ROOT_ADMIN") || a.getAuthority().equals("ROLE_SYSTEM_ADMIN"));
        return isAdmin || isInDept(auth, deptName);
    }
}