package com.barangay.auth.controller;

import com.barangay.auth.dto.*;
import com.barangay.auth.service.AuthService;
import com.barangay.zshared.util.NetworkUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request, NetworkUtil.getClientIp()));
    }


    @PostMapping("/verify-mfa")
    public ResponseEntity<LoginResponse> verifyMfa(@Valid @RequestBody MfaRequest request) {
        return ResponseEntity.ok(authService.verifyMfa(request, NetworkUtil.getClientIp()));
    }

    @PostMapping("/send-backup-otp")
    public ResponseEntity<Void> sendBackupOtp(@RequestParam String email) {
        authService.sendBackupOtp(email,NetworkUtil.getClientIp());
        return ResponseEntity.ok().build();
    }


    @PostMapping("/resend-otp")
    public ResponseEntity<Void> resendOtp(@RequestParam String email) {
        authService.resendOtp(email,NetworkUtil.getClientIp());
        return ResponseEntity.ok().build();
    }


    @PostMapping("/change-password-new-account")
    public ResponseEntity<LoginResponse> changePasswordNewAccount(@Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(authService.changePasswordNewAccount(request,NetworkUtil.getClientIp()));
    }

    @PostMapping("/backup-email/initiate")
    public ResponseEntity<Void>initiateBackupEmailSetup(@RequestParam String primaryEmail, @RequestParam String backupEmail) {
        authService.initiateBackupEmailSetup(primaryEmail,backupEmail,NetworkUtil.getClientIp());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/backup-email/verify")
    public ResponseEntity<Void>verifyAndSaveBackupEmail(@RequestParam String primaryEmail,@RequestParam String backupEmail,@RequestParam String code) {
        authService.verifyAndSaveBackupEmail(primaryEmail,backupEmail,code,NetworkUtil.getClientIp());
        return ResponseEntity.ok().build();

    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void>initiateForgotPassword(String email) {
        authService.initiateForgotPassword(email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password/verify-code")
    public ResponseEntity<Void>verifyResetCode(@Valid @RequestBody VerifyCodeRequest request){
        authService.verifyResetCode(request);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/forgot-password/reset")
    public ResponseEntity<Void>completePasswordReset(ResetPasswordRequest request){
        authService.completePasswordReset(request,NetworkUtil.getClientIp());
        return ResponseEntity.ok().build();
    }







}