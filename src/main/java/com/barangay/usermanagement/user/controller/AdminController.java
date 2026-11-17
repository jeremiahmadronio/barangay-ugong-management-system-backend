package com.barangay.usermanagement.user.controller;

import com.barangay.usermanagement.user.dto.*;
import com.barangay.usermanagement.user.service.AdminService;
import com.barangay.zshared.audit.annotations.LogAudit;
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
 * Controller exposing endpoints for System User (Officers and Staff) operations:
 * dashboard statistics, directory search and table filtering, account details,
 * provisioning, updates, lock management, and archive/trash lifecycle.
 */
@RestController
@RequestMapping({"/api/v1/system-users", "/api/v1/admin/users"})
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * Retrieves dashboard statistics for system users with month-over-month percentage changes.
     */
    @GetMapping("/stats")
    public ResponseEntity<UserManagementStatsResponse> getSystemUserStats() {
        return ResponseEntity.ok(adminService.getSystemUserStats());
    }

    /**
     * Retrieves the paginated or filtered list of system users based on search keywords,
     * department, and status filters.
     */
    @GetMapping("/table")
    public ResponseEntity<List<UserTableResponse>> getSystemUsersTable(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String departmentName,
            @RequestParam(required = false) UserStatus status
    ) {
        UserFilterRequest filter = new UserFilterRequest(search, null, departmentName, status);
        return ResponseEntity.ok(adminService.getFilteredSystemUsersTable(filter));
    }

    /**
     * Retrieves full profile details of a specific system user by their ID.
     */
    @GetMapping("/{userId}/details")
    public ResponseEntity<UserDetailResponse> getSystemUserDetail(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.getSystemUserDetail(userId));
    }

    /**
     * Creates a new officer or staff user account and sends initial login credentials via email.
     */
    @PostMapping
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "CREATE_SYSTEM_USER", severity = Severity.INFO)
    public ResponseEntity<UserResponse> createSystemUser(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = adminService.createSystemUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates profile, department, and role details of an existing system user.
     */
    @PutMapping("/{userId}")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "UPDATE_SYSTEM_USER", severity = Severity.INFO)
    public ResponseEntity<UserResponse> updateSystemUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return ResponseEntity.ok(adminService.updateSystemUser(userId, request));
    }

    /**
     * Updates the active or inactive status of a system user.
     */
    @PatchMapping("/{userId}/status")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "UPDATE_SYSTEM_USER_STATUS", severity = Severity.INFO)
    public ResponseEntity<UserResponse> updateSystemUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UserStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(adminService.updateSystemUserStatus(userId, request));
    }

    /**
     * Toggles the lock state of a system user account.
     */
    @PatchMapping("/{userId}/lock")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "LOCK_UNLOCK_SYSTEM_USER", severity = Severity.WARNING)
    public ResponseEntity<UserResponse> lockUnlockSystemUser(
            @PathVariable Long userId,
            @RequestBody UserLockRequest request
    ) {
        return ResponseEntity.ok(adminService.toggleSystemUserLock(userId, request));
    }

    /**
     * Moves a system user account to the archive vault.
     */
    @PatchMapping("/{userId}/archive")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "ARCHIVE_SYSTEM_USER", severity = Severity.INFO)
    public ResponseEntity<UserResponse> archiveSystemUser(
            @PathVariable Long userId,
            @RequestBody(required = false) UserArchiveRequest request
    ) {
        return ResponseEntity.ok(adminService.archiveSystemUser(userId, request));
    }

    /**
     * Restores an archived system user account back to active service.
     */
    @PatchMapping("/{userId}/restore")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "RESTORE_SYSTEM_USER", severity = Severity.INFO)
    public ResponseEntity<UserResponse> restoreSystemUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.restoreSystemUser(userId));
    }

    /**
     * Moves a system user account into trash (soft delete).
     */
    @DeleteMapping("/{userId}")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "SOFT_DELETE_SYSTEM_USER", severity = Severity.INFO)
    public ResponseEntity<Void> softDeleteSystemUser(@PathVariable Long userId) {
        adminService.softDeleteSystemUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Permanently deletes a system user account and purges associated records from the database.
     */
    @DeleteMapping("/{userId}/permanent")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "PERMANENT_DELETE_SYSTEM_USER", severity = Severity.CRITICAL)
    public ResponseEntity<Void> permanentDeleteSystemUser(@PathVariable Long userId) {
        adminService.permanentDeleteSystemUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves overall archive and trash statistics for system users.
     */
    @GetMapping("/archive/stats")
    public ResponseEntity<ArchiveStatsDTO> getArchiveStats() {
        return ResponseEntity.ok(adminService.getArchiveStats());
    }

    /**
     * Lists all archived system users with an optional search term.
     */
    @GetMapping("/archive/users")
    public ResponseEntity<List<Map<String, Object>>> getArchivedUsers(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(adminService.getArchivedUsers(search));
    }

    /**
     * Lists all soft-deleted system users currently in trash with an optional search term.
     */
    @GetMapping("/archive/trash/users")
    public ResponseEntity<List<Map<String, Object>>> getTrashUsers(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(adminService.getTrashUsers(search));
    }
}
