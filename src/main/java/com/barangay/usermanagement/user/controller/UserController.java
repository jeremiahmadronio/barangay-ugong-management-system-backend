package com.barangay.usermanagement.user.controller;

import com.barangay.usermanagement.user.dto.*;
import com.barangay.usermanagement.user.service.UserService;
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

import java.util.List;

/**
 * Controller exposing endpoints for User Account creation/update (Officers),
 * Admin Account creation/update, User Profile Detail viewing, Dashboard stats, Table queries, and Status/Lock/Archive management.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final com.barangay.contentmanagement.service.SystemSettingService systemSettingService;

    /**
     * Endpoint to check if a system email address is taken by an active non-archived user.
     */
    @GetMapping("/check-email")
    public ResponseEntity<Boolean> checkEmailExists(@RequestParam String email) {
        return ResponseEntity.ok(userService.checkEmailExists(email));
    }

    /**
     * Endpoint to fetch detailed User Profile for viewing.
     */
    @GetMapping("/{userId}/details")
    public ResponseEntity<UserDetailResponse> getUserDetail(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserDetail(userId));
    }

    /**
     * Endpoint to fetch User Management Dashboard statistics with Month-over-Month trend percentages.
     */
    @GetMapping("/stats")
    public ResponseEntity<UserManagementStatsResponse> getUserStats() {
        return ResponseEntity.ok(userService.getUserManagementStats());
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
        return ResponseEntity.ok(userService.getFilteredUsersTable(filter));
    }

    /**
     * Endpoint to update an existing Officer user account.
     */
    @PutMapping("/{userId}")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "UPDATE_OFFICER", severity = Severity.INFO)
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return ResponseEntity.ok(userService.updateUser(userId, request));
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
        return ResponseEntity.ok(userService.updateAdmin(userId, request));
    }

    /**
     * Endpoint to fetch the current authenticated user's profile details.
     */
    @GetMapping("/me")
    public ResponseEntity<UserDetailResponse> getCurrentUserProfile() {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessRuleException("User must be authenticated.");
        }
        return ResponseEntity.ok(userService.getUserDetail(currentUser.getId()));
    }

    /**
     * Endpoint to partially update personal information for the current authenticated user.
     * Only altered fields are updated in the database. System email is immutable.
     */
    @PutMapping("/profile")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "UPDATE_PERSONAL_INFO", severity = Severity.INFO)
    public ResponseEntity<UserResponse> updateCurrentUserPersonalInfo(
            @Valid @RequestBody UpdatePersonalInfoRequest request
    ) {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessRuleException("User must be authenticated to update profile.");
        }
        return ResponseEntity.ok(userService.updatePersonalInfo(currentUser.getId(), request));
    }

    /**
     * Endpoint to partially update personal information by userId.
     */
    @PutMapping("/{userId}/profile")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "UPDATE_PERSONAL_INFO", severity = Severity.INFO)
    public ResponseEntity<UserResponse> updatePersonalInfo(
            @PathVariable Long userId,
            @Valid @RequestBody UpdatePersonalInfoRequest request
    ) {
        return ResponseEntity.ok(userService.updatePersonalInfo(userId, request));
    }

    /**
     * Endpoint to upload/update profile photo for the current authenticated user.
     */
    @PostMapping(value = "/profile/photo", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "UPDATE_PROFILE_PHOTO", severity = Severity.INFO)
    public ResponseEntity<java.util.Map<String, String>> uploadCurrentUserProfilePhoto(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file
    ) {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessRuleException("User must be authenticated to update profile photo.");
        }
        String photoBase64 = userService.updateProfilePhoto(currentUser.getId(), file);
        return ResponseEntity.ok(java.util.Map.of(
                "photo", photoBase64,
                "message", "Profile photo updated successfully."
        ));
    }

    /**
     * Endpoint to upload/update profile photo for a user by ID.
     */
    @PostMapping(value = "/{userId}/photo", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "UPDATE_PROFILE_PHOTO", severity = Severity.INFO)
    public ResponseEntity<java.util.Map<String, String>> uploadUserProfilePhoto(
            @PathVariable Long userId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file
    ) {
        String photoBase64 = userService.updateProfilePhoto(userId, file);
        return ResponseEntity.ok(java.util.Map.of(
                "photo", photoBase64,
                "message", "Profile photo updated successfully."
        ));
    }

    /**
     * Endpoint to fetch profile photo binary image for a user.
     */
    @GetMapping(value = "/{userId}/photo", produces = {org.springframework.http.MediaType.IMAGE_JPEG_VALUE, org.springframework.http.MediaType.IMAGE_PNG_VALUE})
    public ResponseEntity<byte[]> getUserPhoto(@PathVariable Long userId) {
        byte[] photoBytes = userService.getUserPhotoBytes(userId);
        if (photoBytes == null || photoBytes.length == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "max-age=3600")
                .body(photoBytes);
    }

    /**
     * Endpoint to change password for the current authenticated user.
     */
    @PostMapping("/change-password")
    @LogAudit(module = AuditModule.SECURITY, action = "CHANGE_PASSWORD", severity = Severity.INFO)
    public ResponseEntity<java.util.Map<String, String>> changeCurrentUserPassword(
            @Valid @RequestBody UpdatePasswordRequest request
    ) {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessRuleException("User must be authenticated to change password.");
        }
        userService.changePassword(currentUser.getId(), request);
        return ResponseEntity.ok(java.util.Map.of("message", "Password changed successfully."));
    }

    /**
     * Endpoint to change password for a specific user by ID.
     */
    @PostMapping("/{userId}/change-password")
    @LogAudit(module = AuditModule.SECURITY, action = "CHANGE_PASSWORD", severity = Severity.INFO)
    public ResponseEntity<java.util.Map<String, String>> changeUserPassword(
            @PathVariable Long userId,
            @Valid @RequestBody UpdatePasswordRequest request
    ) {
        userService.changePassword(userId, request);
        return ResponseEntity.ok(java.util.Map.of("message", "Password changed successfully."));
    }

    /**
     * Endpoint to fetch the current system-wide Global MFA status.
     */
    @GetMapping("/global-mfa")
    public ResponseEntity<java.util.Map<String, Object>> getGlobalMfaStatus() {
        boolean enabled = systemSettingService.isGlobalMfaEnforced();
        return ResponseEntity.ok(java.util.Map.of("globalMfaEnabled", enabled));
    }

    /**
     * Endpoint to toggle or set system-wide Global MFA enforcement status.
     */
    @PutMapping("/global-mfa")
    @LogAudit(module = AuditModule.SECURITY, action = "TOGGLE_GLOBAL_MFA", severity = Severity.WARNING)
    public ResponseEntity<java.util.Map<String, Object>> updateGlobalMfa(
            @RequestBody java.util.Map<String, Boolean> request
    ) {
        Boolean enabled = request.get("enabled");
        if (enabled == null) {
            throw new BusinessRuleException("Field 'enabled' is required.");
        }
        systemSettingService.setGlobalMfaEnforced(enabled);
        return ResponseEntity.ok(java.util.Map.of(
                "globalMfaEnabled", enabled,
                "message", enabled
                        ? "System-wide Global MFA has been successfully enabled."
                        : "System-wide Global MFA has been successfully disabled."
        ));
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
        return ResponseEntity.ok(userService.updateUserStatus(userId, request));
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
        return ResponseEntity.ok(userService.lockUnlockUser(userId, request));
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
        return ResponseEntity.ok(userService.archiveUser(userId, request));
    }

    /**
     * Endpoint to restore an archived user account.
     */
    @PatchMapping("/{userId}/restore")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "RESTORE_USER", severity = Severity.INFO)
    public ResponseEntity<UserResponse> restoreUser(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.restoreUser(userId));
    }

    /**
     * Endpoint to permanently delete a user account.
     */
    @DeleteMapping("/{userId}")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "DELETE_USER", severity = Severity.CRITICAL)
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint to create a new Officer user account.
     */
    @PostMapping
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "CREATE_OFFICER", severity = Severity.INFO)
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint to create a new Admin account.
     */
    @PostMapping("/create-admin")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "CREATE_ADMIN", severity = Severity.INFO)
    public ResponseEntity<UserResponse> createAdmin(@Valid @RequestBody AdminCreateRequest request) {
        UserResponse response = userService.createAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
