package com.barangay.user_management.permission.service;

import com.barangay.user_management.permission.dto.PermissionResponse;
import com.barangay.user_management.permission.model.Permission;
import com.barangay.user_management.permission.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for retrieving permissions for UI display and checkbox management.
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    /**
     * Retrieves all system permissions formatted for checkbox selection.
     */
    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAllByOrderByModuleAscPermissionNameAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private PermissionResponse mapToResponse(Permission perm) {
        return new PermissionResponse(
                perm.getId(),
                perm.getPermissionName(),
                perm.getModule()
        );
    }
}
