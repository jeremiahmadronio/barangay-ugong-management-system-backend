package com.barangay.usermanagement.user.controller;

import com.barangay.contentmanagement.service.SystemSettingService;
import com.barangay.usermanagement.user.dto.*;
import com.barangay.usermanagement.user.service.SystemAdminService;
import com.barangay.zshared.audit.annotations.LogAudit;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;
import com.barangay.zshared.enumerated.UserStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.exception.BusinessRuleException;
import com.barangay.zshared.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.barangay.auth.service.TotpSetUpService;
import com.barangay.auth.dto.TotpSetupResponse;
import com.barangay.auth.dto.TotpConfirmationRequest;
import com.barangay.auth.dto.TotpEnableSuccessResponse;
import com.barangay.zshared.util.NetworkUtil;
import java.util.List;

/**
 * Controller exposing endpoints for User Account creation/update (Officers),
 * Admin Account creation/update, User Profile Detail viewing, Dashboard stats, Table queries, and Status/Lock/Archive management.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class SystemAdminController {

    private final SystemAdminService systemAdminService;


    /**
     * Endpoint to check if a system email address is taken by an active non-archived user.
     */
    @GetMapping("/check-email")
    public ResponseEntity<Boolean> checkEmailExists(@RequestParam String email) {
        return ResponseEntity.ok(systemAdminService.checkEmailExists(email));
    }

    /**
     * Endpoint to fetch detailed User Profile for viewing.
     */
    @GetMapping("/{userId}/details")
    public ResponseEntity<UserDetailResponse> getUserDetail(@PathVariable Long userId) {
        return ResponseEntity.ok(systemAdminService.getUserDetail(userId));
    }

    /**
     * Endpoint to fetch User Management Dashboard statistics with Month-over-Month trend percentages.
     */
    @GetMapping("/stats")
    public ResponseEntity<UserManagementStatsResponse> getUserStats() {
        return ResponseEntity.ok(systemAdminService.getUserManagementStats());
    }

    /**
     * Endpoint to fetch the User Management table list with complete filtering.
     */
    @GetMapping("/table")
    public ResponseEntity<List<UserTableResponse>> getUsersTable(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String departmentName,
            @RequestParam(required = false) UserStatus status
    ) {
        UserFilterRequest filter = new UserFilterRequest(search, roleName, departmentName, status);
        return ResponseEntity.ok(systemAdminService.getFilteredUsersTable(filter));
    }

    /**
     * Endpoint to update an existing user account.
     */
    @PutMapping("/{userId}")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "UPDATE_OFFICER", severity = Severity.INFO)
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return ResponseEntity.ok(systemAdminService.updateUser(userId, request));
    }

    /**
     * Endpoint to update an existing Admin account.
     */
    @PutMapping("/update-admin/{userId}")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "UPDATE_ADMIN", severity = Severity.INFO)
    public ResponseEntity<UserResponse> updateAdmin(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUpdateRequest request
    ) {
        return ResponseEntity.ok(systemAdminService.updateAdmin(userId, request));
    }

    /**
     * Endpoint to update a user's status (ACTIVE / INACTIVE).
     */
    @PatchMapping("/{userId}/status")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "UPDATE_USER_STATUS", severity = Severity.INFO)
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UserStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(systemAdminService.updateUserStatus(userId, request));
    }

    /**
     * Endpoint to lock or unlock a user account.
     */
    @PatchMapping("/{userId}/lock")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "LOCK_UNLOCK_USER", severity = Severity.WARNING)
    public ResponseEntity<UserResponse> lockUnlockUser(
            @PathVariable Long userId,
            @RequestBody UserLockRequest request
    ) {
        return ResponseEntity.ok(systemAdminService.lockUnlockUser(userId, request));
    }

    /**
     * Endpoint to archive a user account (Soft Delete).
     */
    @PatchMapping("/{userId}/archive")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "ARCHIVE_USER", severity = Severity.INFO)
    public ResponseEntity<UserResponse> archiveUser(
            @PathVariable Long userId,
            @RequestBody(required = false) UserArchiveRequest request
    ) {
        return ResponseEntity.ok(systemAdminService.archiveUser(userId, request));
    }

    /**
     * Endpoint to restore an archived user account.
     */
    @PatchMapping("/{userId}/restore")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "RESTORE_USER", severity = Severity.INFO)
    public ResponseEntity<UserResponse> restoreUser(@PathVariable Long userId) {
        return ResponseEntity.ok(systemAdminService.restoreUser(userId));
    }

    /**
     * Endpoint to permanently delete a user account.
     */
    @DeleteMapping("/{userId}")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "DELETE_USER", severity = Severity.CRITICAL)
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        systemAdminService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint to create a new Officer user account.
     */
    @PostMapping
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "CREATE_OFFICER", severity = Severity.INFO)
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = systemAdminService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint to create a new Admin account.
     */
    @PostMapping("/create-admin")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "CREATE_ADMIN", severity = Severity.INFO)
    public ResponseEntity<UserResponse> createAdmin(@Valid @RequestBody AdminCreateRequest request) {
        UserResponse response = systemAdminService.createAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
