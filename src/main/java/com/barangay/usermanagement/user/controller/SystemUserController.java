package com.barangay.usermanagement.user.controller;

import com.barangay.usermanagement.user.dto.*;
import com.barangay.usermanagement.user.service.SystemUserService;
import com.barangay.zshared.audit.annotations.LogAudit;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;
import com.barangay.zshared.enumerated.UserStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller exposing endpoints for System User (Officers/Staff only) operations,
 * including Month-over-Month stats, table queries, account locking/unlocking,
 * archiving/restoring, and soft/permanent deletion.
 */
@RestController
@RequestMapping({"/api/v1/system-users", "/api/v1/admin/users"})
@RequiredArgsConstructor
public class SystemUserController {

    private final SystemUserService systemUserService;

    /**
     * Endpoint to fetch System User Dashboard statistics with MoM trend percentages.
     */
    @GetMapping("/stats")
    public ResponseEntity<UserManagementStatsResponse> getSystemUserStats() {
        return ResponseEntity.ok(systemUserService.getSystemUserStats());
    }

    /**
     * Endpoint to fetch the System User table list with complete search and filtering.
     */
    @GetMapping("/table")
    public ResponseEntity<List<UserTableResponse>> getSystemUsersTable(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String departmentName,
            @RequestParam(required = false) UserStatus status
    ) {
        UserFilterRequest filter = new UserFilterRequest(search, null, departmentName, status);
        return ResponseEntity.ok(systemUserService.getFilteredSystemUsersTable(filter));
    }

    /**
     * Endpoint to fetch detailed System User profile for viewing.
     */
    @GetMapping("/{userId}/details")
    public ResponseEntity<UserDetailResponse> getSystemUserDetail(@PathVariable Long userId) {
        return ResponseEntity.ok(systemUserService.getSystemUserDetail(userId));
    }

    /**
     * Endpoint to create a new System User (Officer) account.
     */
    @PostMapping
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "CREATE_SYSTEM_USER", severity = Severity.INFO)
    public ResponseEntity<UserResponse> createSystemUser(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = systemUserService.createSystemUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint to update an existing System User account.
     */
    @PutMapping("/{userId}")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "UPDATE_SYSTEM_USER", severity = Severity.INFO)
    public ResponseEntity<UserResponse> updateSystemUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return ResponseEntity.ok(systemUserService.updateSystemUser(userId, request));
    }

    /**
     * Endpoint to update a System User's status (ACTIVE / INACTIVE).
     */
    @PatchMapping("/{userId}/status")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "UPDATE_SYSTEM_USER_STATUS", severity = Severity.INFO)
    public ResponseEntity<UserResponse> updateSystemUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UserStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(systemUserService.updateSystemUserStatus(userId, request));
    }

    /**
     * Endpoint to lock or unlock a System User account.
     */
    @PatchMapping("/{userId}/lock")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "LOCK_UNLOCK_SYSTEM_USER", severity = Severity.WARNING)
    public ResponseEntity<UserResponse> lockUnlockSystemUser(
            @PathVariable Long userId,
            @RequestBody UserLockRequest request
    ) {
        return ResponseEntity.ok(systemUserService.lockUnlockSystemUser(userId, request));
    }

    /**
     * Endpoint to archive a System User account (Soft Archive).
     */
    @PatchMapping("/{userId}/archive")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "ARCHIVE_SYSTEM_USER", severity = Severity.INFO)
    public ResponseEntity<UserResponse> archiveSystemUser(
            @PathVariable Long userId,
            @RequestBody(required = false) UserArchiveRequest request
    ) {
        return ResponseEntity.ok(systemUserService.archiveSystemUser(userId, request));
    }

    /**
     * Endpoint to restore an archived System User account back to active.
     */
    @PatchMapping("/{userId}/restore")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "RESTORE_SYSTEM_USER", severity = Severity.INFO)
    public ResponseEntity<UserResponse> restoreSystemUser(@PathVariable Long userId) {
        return ResponseEntity.ok(systemUserService.restoreSystemUser(userId));
    }

    /**
     * Endpoint to soft-delete a System User account (Move to trash).
     */
    @DeleteMapping("/{userId}")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "SOFT_DELETE_SYSTEM_USER", severity = Severity.INFO)
    public ResponseEntity<Void> softDeleteSystemUser(@PathVariable Long userId) {
        systemUserService.softDeleteSystemUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint to permanently delete a System User account.
     */
    @DeleteMapping("/{userId}/permanent")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "PERMANENT_DELETE_SYSTEM_USER", severity = Severity.CRITICAL)
    public ResponseEntity<Void> permanentDeleteSystemUser(@PathVariable Long userId) {
        systemUserService.permanentDeleteSystemUser(userId);
        return ResponseEntity.noContent().build();
    }
}
