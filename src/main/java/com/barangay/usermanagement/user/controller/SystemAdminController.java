package com.barangay.usermanagement.user.controller;

import com.barangay.usermanagement.user.dto.*;
import com.barangay.usermanagement.user.service.SystemAdminService;
import com.barangay.zshared.audit.annotations.LogAudit;
import com.barangay.zshared.dto.ArchiveSettingsDTO;
import com.barangay.zshared.dto.ArchiveStatsDTO;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;
import com.barangay.zshared.enumerated.UserStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller exposing endpoints for comprehensive user management (System Admin):
 * metrics, directory search and table filtering, account provisioning for officers and admins,
 * status updates, lock toggling, archive retention settings, and full lifecycle actions.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class SystemAdminController {

    private final SystemAdminService systemAdminService;

    /**
     * Retrieves dashboard metrics for all managed users with month-over-month trends.
     */
    @GetMapping("/stats")
    public ResponseEntity<UserManagementStatsResponse> getUserStats() {
        return ResponseEntity.ok(systemAdminService.getUserManagementStats());
    }

    /**
     * Retrieves the list of users based on search keywords, role, department, and status filters.
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
     * Retrieves complete profile details for a given user by ID.
     */
    @GetMapping("/{userId}/details")
    public ResponseEntity<UserDetailResponse> getUserDetail(@PathVariable Long userId) {
        return ResponseEntity.ok(systemAdminService.getUserDetail(userId));
    }

    /**
     * Checks if a system email address is already assigned to an existing active user.
     */
    @GetMapping("/check-email")
    public ResponseEntity<Boolean> checkEmailExists(@RequestParam String email) {
        return ResponseEntity.ok(systemAdminService.checkEmailExists(email));
    }

    /**
     * Creates a new officer user account and notifies the user with credentials via email.
     */
    @PostMapping
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "CREATE_OFFICER", severity = Severity.INFO)
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = systemAdminService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Creates a new administrator account with administrative roles and permissions.
     */
    @PostMapping("/create-admin")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "CREATE_ADMIN", severity = Severity.INFO)
    public ResponseEntity<UserResponse> createAdmin(@Valid @RequestBody AdminCreateRequest request) {
        UserResponse response = systemAdminService.createAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates account information, departments, and permissions of an existing officer.
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
     * Updates account information and role assignments of an administrator account.
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
     * Updates the status (active or inactive) of a user account.
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
     * Locks or unlocks a user account to grant or prevent system access.
     */
    @PatchMapping("/{userId}/lock")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "LOCK_UNLOCK_USER", severity = Severity.WARNING)
    public ResponseEntity<UserResponse> lockUnlockUser(
            @PathVariable Long userId,
            @RequestBody UserLockRequest request
    ) {
        return ResponseEntity.ok(systemAdminService.toggleUserLock(userId, request));
    }

    /**
     * Archives a user account and frees up the linked person profile.
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
     * Restores an archived user back into active state.
     */
    @PatchMapping("/{userId}/restore")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "RESTORE_USER", severity = Severity.INFO)
    public ResponseEntity<UserResponse> restoreUser(@PathVariable Long userId) {
        return ResponseEntity.ok(systemAdminService.restoreUser(userId));
    }

    /**
     * Moves a user account into trash (soft delete).
     */
    @DeleteMapping("/{userId}")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "DELETE_USER", severity = Severity.CRITICAL)
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        systemAdminService.softDeleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Permanently deletes a user account from the system.
     */
    @DeleteMapping("/{userId}/permanent")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "DELETE_USER_PERMANENT", severity = Severity.WARNING)
    public ResponseEntity<String> deleteUserPermanent(@PathVariable Long userId) {
        systemAdminService.permanentDeleteUser(userId);
        return ResponseEntity.ok("User deleted permanently");
    }

    /**
     * Retrieves system archive retention configurations.
     */
    @GetMapping("/archive/settings")
    public ResponseEntity<ArchiveSettingsDTO> getArchiveSettings() {
        return ResponseEntity.ok(systemAdminService.getSettings());
    }

    /**
     * Updates system archive retention configurations such as auto-purge days.
     */
    @PutMapping("/archive/settings")
    @LogAudit(module = AuditModule.SYSTEM, action = "UPDATE_ARCHIVE_SETTINGS", severity = Severity.INFO)
    public ResponseEntity<ArchiveSettingsDTO> updateArchiveSettings(@RequestBody ArchiveSettingsDTO request) {
        return ResponseEntity.ok(systemAdminService.updateSettings(request));
    }

    /**
     * Retrieves overall archive and trash statistics for users and residents.
     */
    @GetMapping("/archive/stats")
    public ResponseEntity<ArchiveStatsDTO> getArchiveStats() {
        return ResponseEntity.ok(systemAdminService.getArchiveStats());
    }

    /**
     * Lists all archived users with optional search keywords.
     */
    @GetMapping("/archive/users")
    public ResponseEntity<List<Map<String, Object>>> getArchivedUsers(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(systemAdminService.getArchivedUsers(search));
    }

    /**
     * Lists all soft-deleted users in trash with optional search keywords.
     */
    @GetMapping("/archive/trash/users")
    public ResponseEntity<List<Map<String, Object>>> getTrashUsers(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(systemAdminService.getTrashUsers(search));
    }
}
