package com.barangay.auth.service;


import com.barangay.auth.dto.TotpConfirmationRequest;
import com.barangay.auth.dto.TotpEnableSuccessResponse;
import com.barangay.auth.dto.TotpSetupResponse;
import com.barangay.user_management.user.model.User;
import com.barangay.user_management.user.repository.UserRepository;
import com.barangay.zshared.audit.service.AuditLogService;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles the optional TOTP (authenticator app) setup flow.

 * This is NOT part of the login flow — this is called after the user
 * is already logged in and wants to enable Google Authenticator.

 * Step 1 — initiateTotpSetup()  → returns QR code for user to scan
 * Step 2 — confirmAndEnable()   → verifies the code and saves the secret
 */
@Service
@RequiredArgsConstructor
public class TotpSetUpService {

    private final UserRepository userRepository;
    private final TotpService totpService;
    private final AuditLogService auditLogService;

    /**
     * Step 1 — Generates a TOTP secret and QR code.
     * The user scans this QR code in their authenticator app (Google Authenticator, etc.)
     * The secret is temporary — only saved after the user confirms in Step 2.
     *
     * @param userId the ID of the currently logged-in user
     * @return MfaSetupResponse containing the secret and QR code image
     */
    public TotpSetupResponse initiateTotpSetup(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found."));

        // Generate a new random TOTP secret
        String secret = totpService.generateSecret();

        // Generate QR code for the authenticator app to scan
        String qrCode = totpService.generateQrCodeBase64(secret, user.getSystemEmail());

        return new TotpSetupResponse(secret, qrCode);
    }

    /**
     * Step 2 — Confirms the TOTP setup by verifying the user's first code.
     * If valid, saves the secret and marks TOTP as enabled for the user.
     *
     * @param userId    the ID of the currently logged-in user
     * @param request   contains the secret from Step 1 and the 6-digit code from the app
     * @param ipAddress the client's IP address for audit logging
     * @return MfaEnableSuccessResponse with success status
     */
    @Transactional
    public TotpEnableSuccessResponse confirmAndEnable(Long userId, TotpConfirmationRequest request, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found."));

        // Verify the code matches the secret
        boolean isValid = totpService.verifyCode(request.secret(), request.code());
        if (!isValid) {
            throw new BadCredentialsException("Invalid verification code. TOTP setup failed.");
        }

        // Save the secret and enable TOTP
        user.setTotpSecret(request.secret());
        user.setTotpEnabled(true);
        userRepository.save(user);

        auditLogService.log(
                user,
                AuditModule.SECURITY,
                "TOTP_ENABLED",
                Severity.INFO,
                "User enabled authenticator app",
                null,
                null
        );

        return new TotpEnableSuccessResponse("TOTP_ENABLED_SUCCESSFULLY");
    }

    /**
     * Disables TOTP for the user and clears the stored secret.
     * Useful if the user loses their authenticator app.
     *
     * @param userId    the ID of the currently logged-in user
     * @param ipAddress the client's IP address for audit logging
     */
    @Transactional
    public void disableTotp(Long userId, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found."));

        user.setTotpSecret(null);
        user.setTotpEnabled(false);
        userRepository.save(user);

        auditLogService.log(
                user,
                AuditModule.SECURITY,
                "TOTP_DISABLED",
                Severity.WARNING,
                "User disabled authenticator app",
                null,
                null
        );
    }
}
