package com.barangay.usermanagement.user.dto;

import com.barangay.usermanagement.department.dto.DepartmentResponse;
import com.barangay.usermanagement.permission.dto.PermissionResponse;
import com.barangay.zshared.enumerated.UserStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detailed DTO response payload for viewing a complete User Profile.
 */
public record UserDetailResponse(
        // 1. Account & System Info
        Long id,
        String photoBase64,
        String username,
        String systemEmail,
        String systemBackupEmail,
        Long roleId,
        String roleName,
        String accountType,
        UserStatus status,
        Boolean isLocked,
        Boolean isNewAccount,

        // 2. Personal Information (Linked Resident / Person)
        PersonSummaryInfo personInfo,

        // 3. Department & Permissions Access
        List<DepartmentResponse> departments,
        List<PermissionResponse> customPermissions,

        // 4. Security Insights & Activity
        LocalDateTime lastLoginAt,
        Integer failedAttempts,
        LocalDateTime lockUntil,
        String statusRemarks,
        List<UserActivityResponse> recentActivities,

        // 5. Audit Trail Timestamps
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime archivedAt,
        String archiveRemarks
) {
    /**
     * Nested record for User Audit Logs / Activity history.
     */
    public record UserActivityResponse(
            Long id,
            String actionTaken,
            String module,
            String severity,
            String reason,
            String ipAddress,
            LocalDateTime timestamp
    ) {}
    /**
     * Nested record for Person / Resident summary details.
     */
    public record PersonSummaryInfo(
            Long personId,
            String fullName,
            String firstName,
            String middleName,
            String lastName,
            String suffix,
            String gender,
            LocalDate birthDate,
            Integer age,
            String civilStatus,
            String contactNumber,
            String completeAddress,
            String occupation
    ) {}
}
