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

/**
 * Controller exposing endpoints for personal profile management:
 * view personal profile, update personal information and password,
 * upload and retrieve profile picture, and setup two-factor authentication (TOTP).
 */
@RestController
@RequestMapping("/api/v1/user-profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final TotpSetUpService totpSetUpService;

    /**
     * Retrieves the profile details of the currently authenticated user.
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
     * Updates personal information for the current user.
     */
    @PutMapping("/profile")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "UPDATE_PERSONAL_INFO", severity = Severity.INFO)
    public ResponseEntity<UserResponse> updateCurrentUserPersonalInfo(
            @Valid @RequestBody UpdatePersonalInfoRequest request
    ) {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessRuleException("User must be authenticated.");
        }
        return ResponseEntity.ok(userProfileService.updatePersonalInfo(currentUser.getId(), request));
    }

    /**
     * Changes the password of the current authenticated user after verifying their old password.
     */
    @PutMapping("/password")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "UPDATE_PASSWORD", severity = Severity.WARNING)
    public ResponseEntity<Map<String, String>> updateCurrentUserPassword(
            @Valid @RequestBody UpdatePasswordRequest request
    ) {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessRuleException("User must be authenticated.");
        }
        userProfileService.updatePassword(currentUser.getId(), request);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    /**
     * Uploads and updates the avatar photo for the currently authenticated user.
     */
    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "UPDATE_PROFILE_PHOTO", severity = Severity.INFO)
    public ResponseEntity<Map<String, String>> uploadCurrentUserPhoto(@RequestParam("file") MultipartFile file) {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessRuleException("User must be authenticated.");
        }
        String base64Image = userProfileService.uploadProfilePhoto(currentUser.getId(), file);
        return ResponseEntity.ok(Map.of("photo", base64Image));
    }

    /**
     * Retrieves the binary profile photo of the currently authenticated user.
     */
    @GetMapping("/photo")
    public ResponseEntity<byte[]> getCurrentUserPhoto() {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessRuleException("User must be authenticated.");
        }
        byte[] photoBytes = userProfileService.getProfilePhotoBytes(currentUser.getId());
        if (photoBytes == null || photoBytes.length == 0) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        return ResponseEntity.ok().headers(headers).body(photoBytes);
    }

    /**
     * Initiates TOTP setup and generates a QR code image for two-factor authentication.
     */
    @PostMapping("/totp/setup")
    public ResponseEntity<TotpSetupResponse> setupTotp() {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessRuleException("User must be authenticated.");
        }
        return ResponseEntity.ok(totpSetUpService.initiateTotpSetup(currentUser.getId()));
    }

    /**
     * Verifies the submitted TOTP code to activate two-factor authentication for the account.
     */
    @PostMapping("/totp/enable")
    @LogAudit(module = AuditModule.USER_MANAGEMENT, action = "ENABLE_TOTP", severity = Severity.WARNING)
    public ResponseEntity<TotpEnableSuccessResponse> enableTotp(@Valid @RequestBody TotpConfirmationRequest request) {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessRuleException("User must be authenticated.");
        }
        String ipAddress = NetworkUtil.getClientIp();
        return ResponseEntity.ok(totpSetUpService.confirmAndEnable(currentUser.getId(), request, ipAddress));
    }
}
