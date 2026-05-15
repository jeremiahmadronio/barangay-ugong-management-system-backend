package com.barangay.auth.service;

import com.barangay.auth.dto.*;
import com.barangay.user_management.user.model.User;
import com.barangay.user_management.user.repository.UserRepository;
import com.barangay.zshared.audit.service.AuditLogService;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;
import com.barangay.zshared.enumerated.UserStatus;
import com.barangay.zshared.exception.BusinessRuleException;
import com.barangay.zshared.security.CustomUserDetails;
import com.barangay.zshared.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles the full login flow — credential validation, account checks,
 * OTP delivery, MFA verification, and JWT generation.
 */
@Service
@RequiredArgsConstructor
public class AuthService {


    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuditLogService auditLogService;
    private final MfaService mfaService;
    private final TotpService totpService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Handles the initial login step.
     * Checks account status, verifies credentials, then sends an OTP to the user's email.
     * Returns MFA_REQUIRED if credentials are valid.
     */
    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress) {

        // Find user by email
        User user = userRepository.findBySystemEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("No account found with this email."));

        // Check if account is locked
        if (Boolean.TRUE.equals(user.getIsLocked())) {
            if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
                long minutesLeft = Math.max(1, ChronoUnit.MINUTES.between(LocalDateTime.now(), user.getLockUntil()));

                auditLogService.log(
                        user, AuditModule.SECURITY, "LOGIN_ATTEMPT_LOCKED",
                        Severity.WARNING, "Attempt on locked account", ipAddress,null, null
                );
                throw new LockedException("Account is temporarily locked. Try again in " + minutesLeft + " minutes.");
            }
            // Auto-unlock if lock period has expired
            user.setIsLocked(false);
            user.setLockUntil(null);
            userRepository.saveAndFlush(user);
        }

        //  Check if account is active
        if (user.getStatus() != UserStatus.ACTIVE) {
            auditLogService.log(
                    user, AuditModule.SECURITY, "LOGIN_ATTEMPT_INACTIVE",
                    Severity.WARNING, "Attempt on inactive account: " + user.getStatus(), ipAddress,null, null
            );
            throw new DisabledException("Account is inactive. Contact your administrator.");
        }

        if (Boolean.TRUE.equals(user.getIsArchived())) {
            auditLogService.log(
                    user, AuditModule.SECURITY, "LOGIN_ATTEMPT_ARCHIVED",
                    Severity.WARNING, "Attempt on archived account", ipAddress,null, null
            );
            throw new DisabledException("This account has been removed. Contact your administrator.");
        }

        //  Verify credentials
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (BadCredentialsException e) {
            handleFailedAttempt(user, ipAddress);
            // handleFailedAttempt will throw the appropriate exception
            throw e;
        }

        // Credentials valid — reset failed attempts
        user.setFailedAttempts(0);
        user.setIsLocked(false);
        user.setLockUntil(null);
        user.setLastFailedAttempt(null);

        // Generate and send OTP
        String code = mfaService.generateCode();
        user.setMfaCode(code);
        user.setMfaExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        mfaService.sendMfaEmail(user.getSystemEmail(), code);

        auditLogService.log(
                user, AuditModule.SECURITY, "LOGIN_OTP_SENT",
                Severity.INFO, "OTP sent to primary email", ipAddress,null, null
        );

        return new LoginResponse(
                "MFA_REQUIRED",
                user.getId(),
                user.getRole().getRoleName(),
                null,
                null,
                Boolean.TRUE.equals(user.getTotpEnabled()),
                user.getSystemBackupEmail() != null
        );
    }


    /**
     * Verifies the MFA code submitted by the user.
     * Supports EMAIL OTP and TOTP (authenticator app).
     * Returns JWT on success.
     */
    @Transactional
    public LoginResponse verifyMfa(MfaRequest request,String ipAddress) {

        User user = userRepository.findBySystemEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("No account found with this email."));

        boolean isVerified = false;

        // Verify based on MFA type
        switch (request.type()) {

            case TOTP -> {
                if (!Boolean.TRUE.equals(user.getTotpEnabled()) || user.getTotpSecret() == null) {
                    throw new BadCredentialsException("Authenticator app is not set up for this account.");
                }
                isVerified = totpService.verifyCode(user.getTotpSecret(), request.code());
            }

            case EMAIL, BACKUP -> {
                isVerified = user.getMfaCode() != null
                        && user.getMfaCode().equals(request.code())
                        && user.getMfaExpiry() != null
                        && user.getMfaExpiry().isAfter(LocalDateTime.now());
            }

            default -> throw new BadCredentialsException("Unsupported MFA type.");
        }

        // If verification failed
        if (!isVerified) {
            auditLogService.log(
                    user, AuditModule.SECURITY, "MFA_VERIFICATION_FAILED",
                    Severity.WARNING, "Failed MFA attempt via " + request.type(), ipAddress,null, null
            );
            throw new BadCredentialsException("Invalid or expired verification code. Please try again.");
        }

        // Clear MFA code after successful verification
        user.setMfaCode(null);
        user.setMfaExpiry(null);
        user.setFailedAttempts(0);
        user.setIsLocked(false);
        user.setLockUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // New account — must change password first
        if (Boolean.TRUE.equals(user.getIsNewAccount())) {
            return new LoginResponse(
                    "CHANGE_PASSWORD_REQUIRED",
                    user.getId(),
                    user.getRole().getRoleName(),
                    null,
                    null,
                    Boolean.TRUE.equals(user.getTotpEnabled()),
                    user.getSystemBackupEmail() != null
            );
        }

        // Generate JWT and return success
        String token = generateToken(user);

        auditLogService.log(
                user, AuditModule.SECURITY, "LOGIN_SUCCESS",
                Severity.INFO, "Successful login via " + request.type(), ipAddress,null, null
        );

        return new LoginResponse(
                "SUCCESS",
                user.getId(),
                user.getRole().getRoleName(),
                getDepartmentNames(user),
                token,
                Boolean.TRUE.equals(user.getTotpEnabled()),
                user.getSystemBackupEmail() != null
        );
    }



    @Transactional
    public void sendBackupOtp(String email,String ipAddress) {
        User user = userRepository.findBySystemEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No account found."));

        if (user.getSystemBackupEmail() == null) {
            throw new BusinessRuleException("No backup email configured.");
        }

        // Reuse or Generate logic
        if (isOtpExpired(user)) {
            String code = mfaService.generateCode();
            user.setMfaCode(code);
            user.setMfaExpiry(LocalDateTime.now().plusMinutes(5));
            userRepository.save(user);
        }

        mfaService.sendMfaEmail(user.getSystemBackupEmail(), user.getMfaCode());

        auditLogService.log(user, AuditModule.SECURITY, "BACKUP_OTP_SENT",
                Severity.INFO, "OTP sent to backup email",ipAddress, null, null);
    }

    @Transactional
    public void resendOtp(String email,String ipAddress) {
        User user = userRepository.findBySystemEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No account found."));

        String code = mfaService.generateCode();
        user.setMfaCode(code);
        user.setMfaExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        mfaService.sendMfaEmail(user.getSystemEmail(), code);

        auditLogService.log(user, AuditModule.SECURITY, "OTP_RESENT",
                Severity.INFO, "OTP resent to primary email", ipAddress,null, null);
    }





    @Transactional
    public LoginResponse changePasswordNewAccount(ChangePasswordRequest request,String ipAddress) {

        User user = userRepository.findBySystemEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("No account found with this email: " + request.email()));

        if(!Boolean.TRUE.equals(user.getIsNewAccount())){
            auditLogService.log(
                    user,
                    AuditModule.SECURITY,
                    "Unauthorized password change",
                    Severity.WARNING,
                    "Unauthorized password change attempt on existing account",
                    ipAddress,
                    null,
                    null
            );
            throw new AccessDeniedException("This endpoint is strictly for new account initialization.");
        }

        if(!request.newPassword().equals(request.confirmPassword())){
            throw new BadCredentialsException("Passwords don't match.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setIsNewAccount(false);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = generateToken(user);

        auditLogService.log(
                user,
                AuditModule.SECURITY,
                "New Account Initialized",
                Severity.INFO, null, ipAddress,null, null
        );
        return new LoginResponse(
                "SUCCESS",
                user.getId(),
                user.getRole().getRoleName(),
                getDepartmentNames(user),
                token,
                Boolean.TRUE.equals(user.getTotpEnabled()),
                user.getSystemBackupEmail() != null
        );
    }


    @Transactional
    public void initiateBackupEmailSetup(String primaryEmail, String backupEmail,String ipAddress) {
        User user = userRepository.findBySystemEmail(primaryEmail)
                .orElseThrow(() -> new UsernameNotFoundException("No account found with this email: " + primaryEmail));

            // Validate backup email format first
            if (backupEmail == null || backupEmail.isBlank()) {
                throw new IllegalArgumentException("Backup email cannot be empty.");
            }

            if (!backupEmail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                throw new IllegalArgumentException("Invalid email format: " + backupEmail);
            }

            // Cannot use same email as primary
            if (backupEmail.equalsIgnoreCase(primaryEmail)) {
                throw new IllegalArgumentException("Backup email cannot be the same as your primary email.");
            }

        String code  = mfaService.generateCode();
        user.setMfaCode(code);
        user.setMfaExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        mfaService.sendMfaEmail(backupEmail, code);

        auditLogService.log(
                user,
                AuditModule.SECURITY,
                "Backup email setup initialized",
                Severity.INFO,
                null,
                ipAddress,
                null,
                null
        );
    }


    @Transactional
    public void verifyAndSaveBackupEmail(String primaryEmail, String backupEmail, String code,String ipAddress) {

        User user = userRepository.findBySystemEmail(primaryEmail)
                .orElseThrow(() -> new UsernameNotFoundException("No account found with this email: " + primaryEmail));

        if (user.getMfaCode() == null
                || !user.getMfaCode().equals(code)
                || user.getMfaExpiry() == null
                || user.getMfaExpiry().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("Invalid or expired verification code.");
        }

        user.setSystemBackupEmail(backupEmail);
        user.setMfaCode(null);
        user.setMfaExpiry(null);
        userRepository.save(user);

        auditLogService.log(
                user, AuditModule.SECURITY, "Backup email set to: " + backupEmail,
                Severity.INFO, null, ipAddress,null, null
        );

    }

    @Transactional
    public void initiateForgotPassword(String email){
        User user = userRepository.findBySystemEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No account found with this email: " + email));

        String code = mfaService.generateCode();
        user.setMfaCode(code);
        user.setMfaExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        mfaService.sendMfaEmail(user.getSystemEmail(), code);
    }



    @Transactional
    public void verifyResetCode(VerifyCodeRequest request) {

        User user = userRepository.findBySystemEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("No account found with this email"));

        if (user.getMfaCode() == null || !user.getMfaCode().equals(request.code())) {
            throw new BadCredentialsException("The verification code is invalid. Please check and try again.");
        }

        if (user.getMfaExpiry() == null || user.getMfaExpiry().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("The verification code has expired. Please request a new one.");
        }
    }


    @Transactional
    public void completePasswordReset(ResetPasswordRequest request, String ipAddress) {

        User user = userRepository.findBySystemEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("No account found with this email"));

        if (user.getMfaCode() == null || !user.getMfaCode().equals(request.code())) {
            throw new BadCredentialsException("Unauthorized reset attempt. Verification failed.");
        }

        if (user.getMfaExpiry() == null || user.getMfaExpiry().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("Session expired. Please restart the password reset process.");
        }

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setMfaCode(null);
        user.setMfaExpiry(null);
        user.setIsNewAccount(false);
        userRepository.save(user);

        auditLogService.log(
                user, AuditModule.SECURITY, "PASSWORD_RESET_SUCCESS",
                Severity.WARNING, "User successfully reset password",ipAddress, null, null
        );
    }














    private boolean isOtpExpired(User user) {
        return user.getMfaCode() == null || user.getMfaExpiry() == null
                || user.getMfaExpiry().isBefore(LocalDateTime.now());
    }

    /**
     * Handles a failed login attempt.
     * Increments the failed attempt counter and locks the account after 10 attempts.
     */
    private void handleFailedAttempt(User user, String ipAddress) {
        int attempts = (user.getFailedAttempts() == null ? 0 : user.getFailedAttempts()) + 1;
        user.setFailedAttempts(attempts);
        user.setLastFailedAttempt(LocalDateTime.now());

        if (attempts >= 10) {
            user.setIsLocked(true);
            user.setLockUntil(LocalDateTime.now().plusMinutes(15));
            userRepository.saveAndFlush(user);

            auditLogService.log(
                    user, AuditModule.SECURITY, "ACCOUNT_LOCKED",
                    Severity.CRITICAL, "Account locked after " + attempts + " failed attempts", ipAddress,null, null
            );

            throw new LockedException("Too many failed attempts. Account locked for 15 minutes.");
        }

        userRepository.saveAndFlush(user);

        auditLogService.log(
                user, AuditModule.SECURITY, "LOGIN_FAILED",
                Severity.WARNING, "Failed login attempt #" + attempts, ipAddress,null, null
        );

        throw new BadCredentialsException("Invalid email or password. Attempt " + attempts + " of 10.");
    }

    /**
     * Generates a JWT token with role, userId, and departments as claims.
     */
    private String generateToken(User user) {
        HashMap<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole().getRoleName());
        extraClaims.put("userId", user.getId());
        extraClaims.put("depts", getDepartmentNames(user));
        return jwtService.generateToken(extraClaims, new CustomUserDetails(user));
    }

    /**
     * Returns the set of department names the user belongs to.
     */
    private Set<String> getDepartmentNames(User user) {
        return user.getDepartments().stream()
                .map(dept -> dept.getName())
                .collect(Collectors.toSet());
    }

}
