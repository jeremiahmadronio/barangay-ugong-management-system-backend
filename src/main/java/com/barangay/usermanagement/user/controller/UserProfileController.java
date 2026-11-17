package com.barangay.usermanagement.user.controller;



import com.barangay.auth.dto.TotpConfirmationRequest;
import com.barangay.auth.dto.TotpEnableSuccessResponse;
import com.barangay.auth.dto.TotpSetupResponse;
import com.barangay.auth.service.TotpSetUpService;
import com.barangay.usermanagement.user.dto.UpdatePasswordRequest;
import com.barangay.usermanagement.user.dto.UpdatePersonalInfoRequest;
import com.barangay.usermanagement.user.dto.UserDetailResponse;
import com.barangay.usermanagement.user.dto.UserResponse;
import com.barangay.usermanagement.user.model.User;
import com.barangay.usermanagement.user.service.UserProfileService;
import com.barangay.zshared.audit.annotations.LogAudit;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;
import com.barangay.zshared.exception.BusinessRuleException;
import com.barangay.zshared.security.SecurityUtils;
import com.barangay.zshared.util.NetworkUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/user-profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final TotpSetUpService totpSetUpService;

    /**
     * Endpoint to fetch the current authenticated user's profile details.
     */
    @GetMapping("/me")
    public ResponseEntity<UserDetailResponse> getCurrentUserProfile() {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessRuleException("User must be authenticated.");
        }
        return ResponseEntity.ok(userProfileService.getUserDetail(currentUser.getId()));
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
        return ResponseEntity.ok(userProfileService.updatePersonalInfo(currentUser.getId(), request));
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
        return ResponseEntity.ok(userProfileService.updatePersonalInfo(userId, request));
    }

    /**
     * Endpoint to upload/update profile photo for the current authenticated user.
     */
    @PostMapping(value = "/profile/photo", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "UPDATE_PROFILE_PHOTO", severity = Severity.INFO)
    public ResponseEntity<Map<String, String>> uploadCurrentUserProfilePhoto(
            @RequestParam("file") MultipartFile file
    ) {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessRuleException("User must be authenticated to update profile photo.");
        }
        String photoBase64 = userProfileService.updateProfilePhoto(currentUser.getId(), file);
        return ResponseEntity.ok(java.util.Map.of(
                "photo", photoBase64,
                "message", "Profile photo updated successfully."
        ));
    }

    /**
     * Endpoint to upload/update profile photo for a user by ID.
     */
    @PostMapping(value = "/{userId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "UPDATE_PROFILE_PHOTO", severity = Severity.INFO)
    public ResponseEntity<Map<String, String>> uploadUserProfilePhoto(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file
    ) {
        String photoBase64 = userProfileService.updateProfilePhoto(userId, file);
        return ResponseEntity.ok(java.util.Map.of(
                "photo", photoBase64,
                "message", "Profile photo updated successfully."
        ));
    }

    /**
     * Endpoint to fetch profile photo binary image for a user.
     */
    @GetMapping(value = "/{userId}/photo", produces = {MediaType.IMAGE_JPEG_VALUE, org.springframework.http.MediaType.IMAGE_PNG_VALUE})
    public ResponseEntity<byte[]> getUserPhoto(@PathVariable Long userId) {
        byte[] photoBytes = userProfileService.getUserPhotoBytes(userId);
        if (photoBytes == null || photoBytes.length == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                .body(photoBytes);
    }

    /**
     * Endpoint to change password for the current authenticated user.
     */
    @PostMapping("/change-password")
    @LogAudit(module = AuditModule.SECURITY, action = "CHANGE_PASSWORD", severity = Severity.INFO)
    public ResponseEntity<Map<String, String>> changeCurrentUserPassword(
            @Valid @RequestBody UpdatePasswordRequest request
    ) {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessRuleException("User must be authenticated to change password.");
        }
        userProfileService.changePassword(currentUser.getId(), request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }

    /**
     * Endpoint to change password for a specific user by ID.
     */
    @PostMapping("/{userId}/change-password")
    @LogAudit(module = AuditModule.SECURITY, action = "CHANGE_PASSWORD", severity = Severity.INFO)
    public ResponseEntity<Map<String, String>> changeUserPassword(
            @PathVariable Long userId,
            @Valid @RequestBody UpdatePasswordRequest request
    ) {
        userProfileService.changePassword(userId, request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }

    /**
     * Endpoint to fetch the current system-wide Global MFA status.
     */
    @GetMapping("/global-mfa")
    public ResponseEntity<Map<String, Object>> getGlobalMfaStatus() {
        boolean enabled = userProfileService.isGlobalMfaEnforced();
        return ResponseEntity.ok(Map.of("globalMfaEnabled", enabled));
    }

    /**
     * Endpoint to toggle or set system-wide Global MFA enforcement status.
     */
    @PutMapping("/global-mfa")
    @LogAudit(module = AuditModule.SECURITY, action = "TOGGLE_GLOBAL_MFA", severity = Severity.WARNING)
    public ResponseEntity<Map<String, Object>> updateGlobalMfa(
            @RequestBody Map<String, Boolean> request
    ) {
        Boolean enabled = request.get("enabled");
        if (enabled == null) {
            throw new BusinessRuleException("Field 'enabled' is required.");
        }
        userProfileService.setGlobalMfaEnforced(enabled);
        return ResponseEntity.ok(java.util.Map.of(
                "globalMfaEnabled", enabled,
                "message", enabled
                        ? "System-wide Global MFA has been successfully enabled."
                        : "System-wide Global MFA has been successfully disabled."
        ));
    }

    /**
     * Endpoint to initiate TOTP setup: generates temporary secret & QR code Base64.
     */
    @GetMapping("/totp/setup")
    public ResponseEntity<TotpSetupResponse> initiateTotpSetup() {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessRuleException("User must be authenticated.");
        }
        return ResponseEntity.ok(totpSetUpService.initiateTotpSetup(currentUser.getId()));
    }

    /**
     * Endpoint to verify 6-digit TOTP code and enable TOTP for the current user.
     */
    @PostMapping("/totp/verify")
    public ResponseEntity<TotpEnableSuccessResponse> confirmAndEnableTotp(
            @Valid @RequestBody TotpConfirmationRequest request
    ) {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessRuleException("User must be authenticated.");
        }
        return ResponseEntity.ok(totpSetUpService.confirmAndEnable(currentUser.getId(), request, NetworkUtil.getClientIp()));
    }

    /**
     * Endpoint to disable TOTP for the current user.
     */
    @PostMapping("/totp/disable")
    public ResponseEntity<java.util.Map<String, String>> disableTotp() {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessRuleException("User must be authenticated.");
        }
        totpSetUpService.disableTotp(currentUser.getId(), NetworkUtil.getClientIp());
        return ResponseEntity.ok(java.util.Map.of("message", "TOTP authentication has been disabled successfully."));
    }
}
