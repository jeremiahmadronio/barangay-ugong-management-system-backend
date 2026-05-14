package com.barangay.auth.controller;

import com.barangay.auth.dto.LoginRequest;
import com.barangay.auth.dto.LoginResponse;
import com.barangay.auth.dto.MfaRequest;
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
        authService.sendBackupOtp(email);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/resend-otp")
    public ResponseEntity<Void> resendOtp(@RequestParam String email) {
        authService.resendOtp(email);
        return ResponseEntity.ok().build();
    }
}