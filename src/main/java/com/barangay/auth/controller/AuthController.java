package com.barangay.auth.controller;

import com.barangay.auth.dto.*;
import com.barangay.auth.service.AuthService;
import com.barangay.zshared.util.NetworkUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller handling user authentication, MFA verification, and password reset workflows.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Authenticates user credentials and initiates MFA if required.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request, NetworkUtil.getClientIp()));
    }

    /**
     * Verifies the submitted MFA OTP or TOTP code and returns access token.
     */
    @PostMapping("/verify-mfa")
    public ResponseEntity<LoginResponse> verifyMfa(@Valid @RequestBody MfaRequest request) {
        return ResponseEntity.ok(authService.verifyMfa(request, NetworkUtil.getClientIp()));
    }

    /**
     * Sends an OTP to the user's registered backup email address.
     */
    @PostMapping("/send-backup-otp")
    public ResponseEntity<Void> sendBackupOtp(@RequestParam String email) {
        authService.sendBackupOtp(email, NetworkUtil.getClientIp());
        return ResponseEntity.ok().build();
    }

    /**
     * Resends a fresh MFA OTP to the user's primary email.
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<Void> resendOtp(@RequestParam String email) {
        authService.resendOtp(email, NetworkUtil.getClientIp());
        return ResponseEntity.ok().build();
    }

    /**
     * Forces initial password change for newly created user accounts.
     */
    @PostMapping("/change-password-new-account")
    public ResponseEntity<LoginResponse> changePasswordNewAccount(@Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(authService.changePasswordNewAccount(request, NetworkUtil.getClientIp()));
    }

    /**
     * Initiates backup email registration by dispatching a verification code.
     */
    @PostMapping("/backup-email/initiate")
    public ResponseEntity<Void> initiateBackupEmailSetup(@RequestParam String primaryEmail, @RequestParam String backupEmail) {
        authService.initiateBackupEmailSetup(primaryEmail, backupEmail, NetworkUtil.getClientIp());
        return ResponseEntity.ok().build();
    }

    /**
     * Verifies confirmation code and attaches backup email to the user account.
     */
    @PostMapping("/backup-email/verify")
    public ResponseEntity<Void> verifyAndSaveBackupEmail(@RequestParam String primaryEmail, @RequestParam String backupEmail, @RequestParam String code) {
        authService.verifyAndSaveBackupEmail(primaryEmail, backupEmail, code, NetworkUtil.getClientIp());
        return ResponseEntity.ok().build();
    }

    /**
     * Initiates forgot-password recovery by sending a reset code.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> initiateForgotPassword(@RequestParam("email") String email) {
        authService.initiateForgotPassword(email);
        return ResponseEntity.ok().build();
    }

    /**
     * Verifies that the supplied password reset code is valid and active.
     */
    @PostMapping("/forgot-password/verify-code")
    public ResponseEntity<Void> verifyResetCode(@Valid @RequestBody VerifyCodeRequest request) {
        authService.verifyResetCode(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Completes password reset using verified token and sets the new password.
     */
    @PostMapping("/forgot-password/reset")
    public ResponseEntity<Void> completePasswordReset(@Valid @RequestBody ResetPasswordRequest request) {
        authService.completePasswordReset(request, NetworkUtil.getClientIp());
        return ResponseEntity.ok().build();
    }
}