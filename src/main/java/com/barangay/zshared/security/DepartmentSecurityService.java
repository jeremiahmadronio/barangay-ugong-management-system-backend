package com.barangay.zshared.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("deptSec")
@RequiredArgsConstructor
public class DepartmentSecurityService {

    public boolean isInDept(Authentication auth, String deptName) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority()
                        .equals("DEPT_" + deptName.toUpperCase()));
    }

    public boolean isOwner(Authentication auth, Long resourceOwnerId) {
        CustomUserDetails details = (CustomUserDetails) auth.getPrincipal();
        return details.user().getId().equals(resourceOwnerId);
    }

    public boolean canAccessDept(Authentication auth, String deptName) {
        boolean isRoot = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ROOT_ADMIN"));
        return isRoot || isInDept(auth, deptName);
    }
}