package com.barangay.auth.service;

import com.barangay.auth.dto.*;
import com.barangay.usermanagement.user.model.User;
import com.barangay.usermanagement.user.repository.UserRepository;
import com.barangay.zshared.audit.service.AuditLogService;
import com.barangay.zshared.enumerated.MfaType;
import com.barangay.zshared.enumerated.UserStatus;
import com.barangay.zshared.exception.BusinessRuleException;
import com.barangay.zshared.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - Unit test")
class AuthServiceTest {

    /** Repository mock — never touches a real database. */
    @Mock private UserRepository userRepository;

    /** JWT utility mock — returns pre-set token strings. */
    @Mock private JwtService jwtService;

    /** Spring Security authentication manager mock. */
    @Mock private AuthenticationManager authenticationManager;

    /** Audit log mock — verifies security events are recorded. */
    @Mock private AuditLogService auditLogService;

    /** OTP generation and email-delivery mock. */
    @Mock private MfaService mfaService;

    /** TOTP (authenticator-app) verification mock. */
    @Mock private TotpService totpService;

    /** BCrypt encoder mock — avoids real hashing overhead. */
    @Mock private PasswordEncoder passwordEncoder;

    /** System setting repository mock. */
    @Mock private com.barangay.contentmanagement.repository.SystemSettingRepository systemSettingRepository;

    /** System under test — all mocks are injected automatically. */
    @InjectMocks
    private AuthService authService;


    /** Simulated client IP address used across all tests. */
    private static final String IP = "127.0.0.1";

    /** Primary email used for the test user. */
    private static final String EMAIL = "sample@gmail.com";

    /** Plain-text password used in login requests. */
    private static final String PASSWORD = "SamplePassword#123";

    /** Sample 6-digit OTP reused across multiple test scenarios. */
    private static final String OTP_CODE = "123456";

   private static final String TOTP_SECRET = "JBSWY3DPEHPK3PXP";

    private static final String JWT_TOKEN = "sampletoken";

    private static final String BACKUP_EMAIL = "backupsample@gmail.com";



    /**
     * Builds a fully active, non-locked, non-archived user with no departments
     * and a stubbed role that returns {@code "ROLE_RESIDENT"}.
     *
     * <p>Tests that need a different state should mutate this object directly
     * (e.g. {@code user.setIsLocked(true)}) before stubbing the repository.
     *
     * @return a ready-to-use {@link User} test fixture
     */
    private User activeUser() {
        User user = new User();
        user.setId(1L);
        user.setSystemEmail(EMAIL);
        user.setPassword("encoded-password");
        user.setStatus(UserStatus.ACTIVE);
        user.setIsLocked(false);
        user.setIsArchived(false);
        user.setIsNewAccount(false);
        user.setTotpEnabled(false);
        user.setFailedAttempts(0);
        user.setDepartments(Set.of());

        var role = mock(com.barangay.usermanagement.role.model.Role.class);
        lenient().when(role.getRoleName()).thenReturn("ROLE_RESIDENT");
        user.setRole(role);
        return user;
    }


    @Nested
    @DisplayName("login()")
    class LoginTests {

        /**
         * Login request reused by every test in this group.
         */
        private LoginRequest request;

        @BeforeEach
        void setUp() {
            request = new LoginRequest(EMAIL, PASSWORD);
        }


        /**
         * Verifies that an email address not present in the database causes an
         * immediate {@link UsernameNotFoundException} without touching any other service.
         */
        @Test
        @DisplayName("Unknown email → UsernameNotFoundException")
        void unknownEmail_throws() {
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request, IP))
                    .isInstanceOf(UsernameNotFoundException.class);
        }


        @Test
        @DisplayName("Locked account within lock window → LockedException")
        void lockedAccount_throws() {
            User user = activeUser();
            user.setIsLocked(true);
            user.setLockUntil(LocalDateTime.now().plusMinutes(10)); // still locked
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.login(request, IP))
                    .isInstanceOf(LockedException.class)
                    .hasMessageContaining("temporarily locked");
        }


        @Test
        @DisplayName("Lock period expired → account auto-unlocked and login continues")
        void expiredLock_autoUnlocks() {
            User user = activeUser();
            user.setIsLocked(true);
            user.setLockUntil(LocalDateTime.now().minusMinutes(1)); // already past
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));
            when(userRepository.saveAndFlush(any())).thenReturn(user);
            when(mfaService.generateCode()).thenReturn(OTP_CODE);

            // authenticate passes silently; save() succeeds
            LoginResponse response = authService.login(request, IP);

            assertThat(user.getIsLocked()).isFalse();
            assertThat(response.status()).isEqualTo("MFA_REQUIRED");
        }


        @Test
        @DisplayName("Inactive account → DisabledException")
        void inactiveAccount_throws() {
            User user = activeUser();
            user.setStatus(UserStatus.INACTIVE);
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.login(request, IP))
                    .isInstanceOf(DisabledException.class)
                    .hasMessageContaining("inactive");
        }

        @Test
        @DisplayName("Archived account → DisabledException")
        void archivedAccount_throws() {
            User user = activeUser();
            user.setIsArchived(true);
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.login(request, IP))
                    .isInstanceOf(DisabledException.class)
                    .hasMessageContaining("removed");
        }

        @Test
        @DisplayName("Wrong password (attempt 1–9) → BadCredentialsException with attempt count")
        void wrongPassword_incrementsAttempts() {
            User user = activeUser();
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));
            doThrow(new BadCredentialsException("bad credentials"))
                    .when(authenticationManager).authenticate(any());

            assertThatThrownBy(() -> authService.login(request, IP))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Attempt 1 of 10");

            assertThat(user.getFailedAttempts()).isEqualTo(1);
        }

        @Test
        @DisplayName("Wrong password on 10th attempt → account locked")
        void tenthFailedAttempt_locksAccount() {
            User user = activeUser();
            user.setFailedAttempts(9); // 9 previous failures
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));
            doThrow(new BadCredentialsException("bad credentials"))
                    .when(authenticationManager).authenticate(any());
            when(userRepository.saveAndFlush(any())).thenReturn(user);

            assertThatThrownBy(() -> authService.login(request, IP))
                    .isInstanceOf(LockedException.class)
                    .hasMessageContaining("locked for 15 minutes");

            assertThat(user.getIsLocked()).isTrue();
            assertThat(user.getLockUntil()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("Valid credentials → OTP sent and MFA_REQUIRED returned")
        void validCredentials_sendOtpAndReturnMfaRequired() {
            User user = activeUser();
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));
            when(mfaService.generateCode()).thenReturn(OTP_CODE);

            LoginResponse response = authService.login(request, IP);

            assertThat(response.status()).isEqualTo("MFA_REQUIRED");
            assertThat(response.userId()).isEqualTo(1L);
            verify(mfaService).sendMfaEmail(EMAIL, OTP_CODE);
        }

        @Test
        @DisplayName("Valid credentials with TOTP enabled → totpEnabled flag is true")
        void validCredentials_totpEnabled_flagIsTrue() {
            User user = activeUser();
            user.setTotpEnabled(true);
            user.setTotpSecret(TOTP_SECRET);
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));
            when(mfaService.generateCode()).thenReturn(OTP_CODE);

            LoginResponse response = authService.login(request, IP);

            assertThat(response.totpEnabled()).isTrue();
        }
    }






    /**
     * Test suite for multi-factor authorization processes (`verifyMfa`).
     * Asserts that secure system sessions are only dispatched upon validation
     * of physical constraints like expiration thresholds and code alignment.
     */
    @Nested
    @DisplayName("verifyMfa()")
    class VerifyMfaTests {

        @Test
        @DisplayName("Unknown email → UsernameNotFoundException")
        void unknownEmail_throws() {
            MfaRequest request = new MfaRequest(EMAIL, OTP_CODE, MfaType.EMAIL);
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyMfa(request, IP))
                    .isInstanceOf(UsernameNotFoundException.class);
        }

        @Test
        @DisplayName("Valid EMAIL OTP → SUCCESS and JWT returned")
        void validEmailOtp_returnsSuccess() {
            User user = activeUser();
            user.setMfaCode(OTP_CODE);
            user.setMfaExpiry(LocalDateTime.now().plusMinutes(5));
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));
            when(jwtService.generateToken(any(), any())).thenReturn(JWT_TOKEN);

            MfaRequest request = new MfaRequest(EMAIL, OTP_CODE, MfaType.EMAIL);
            LoginResponse response = authService.verifyMfa(request, IP);

            assertThat(response.status()).isEqualTo("SUCCESS");
            assertThat(response.token()).isEqualTo(JWT_TOKEN);
        }

        @Test
        @DisplayName("Expired EMAIL OTP → BadCredentialsException")
        void expiredEmailOtp_throws() {
            User user = activeUser();
            user.setMfaCode(OTP_CODE);
            user.setMfaExpiry(LocalDateTime.now().minusMinutes(1)); // already expired
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));

            MfaRequest request = new MfaRequest(EMAIL, OTP_CODE, MfaType.EMAIL);

            assertThatThrownBy(() -> authService.verifyMfa(request, IP))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Invalid or expired");
        }

        @Test
        @DisplayName("Wrong EMAIL OTP code → BadCredentialsException")
        void wrongEmailOtp_throws() {
            User user = activeUser();
            user.setMfaCode("999999");
            user.setMfaExpiry(LocalDateTime.now().plusMinutes(5));
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));

            MfaRequest request = new MfaRequest(EMAIL, OTP_CODE, MfaType.EMAIL); // different code

            assertThatThrownBy(() -> authService.verifyMfa(request, IP))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("Valid TOTP code → SUCCESS and JWT returned")
        void validTotp_returnsSuccess() {
            User user = activeUser();
            user.setTotpEnabled(true);
            user.setTotpSecret(TOTP_SECRET);
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));
            when(totpService.verifyCode(TOTP_SECRET, OTP_CODE)).thenReturn(true);
            when(jwtService.generateToken(any(), any())).thenReturn(JWT_TOKEN);

            MfaRequest request = new MfaRequest(EMAIL, OTP_CODE, MfaType.TOTP);
            LoginResponse response = authService.verifyMfa(request, IP);

            assertThat(response.status()).isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("TOTP not set up on account → BadCredentialsException")
        void totpNotSetUp_throws() {
            User user = activeUser();
            user.setTotpEnabled(false); // TOTP off
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));

            MfaRequest request = new MfaRequest(EMAIL, OTP_CODE, MfaType.TOTP);

            assertThatThrownBy(() -> authService.verifyMfa(request, IP))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("not set up");
        }

        @Test
        @DisplayName("Invalid TOTP code → BadCredentialsException")
        void invalidTotpCode_throws() {
            User user = activeUser();
            user.setTotpEnabled(true);
            user.setTotpSecret(TOTP_SECRET);
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));
            when(totpService.verifyCode(TOTP_SECRET, OTP_CODE)).thenReturn(false);

            MfaRequest request = new MfaRequest(EMAIL, OTP_CODE, MfaType.TOTP);

            assertThatThrownBy(() -> authService.verifyMfa(request, IP))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("New account → CHANGE_PASSWORD_REQUIRED returned after MFA")
        void newAccount_returnsChangePasswordRequired() {
            User user = activeUser();
            user.setIsNewAccount(true);
            user.setMfaCode(OTP_CODE);
            user.setMfaExpiry(LocalDateTime.now().plusMinutes(5));
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));

            MfaRequest request = new MfaRequest(EMAIL, OTP_CODE, MfaType.EMAIL);
            LoginResponse response = authService.verifyMfa(request, IP);

            assertThat(response.status()).isEqualTo("CHANGE_PASSWORD_REQUIRED");
            assertThat(response.token()).isNull();
        }

        @Test
        @DisplayName("Successful MFA → MFA code cleared from user record")
        void successfulMfa_clearsMfaCode() {
            User user = activeUser();
            user.setMfaCode(OTP_CODE);
            user.setMfaExpiry(LocalDateTime.now().plusMinutes(5));
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));
            when(jwtService.generateToken(any(), any())).thenReturn(JWT_TOKEN);

            MfaRequest request = new MfaRequest(EMAIL, OTP_CODE, MfaType.EMAIL);
            authService.verifyMfa(request, IP);

            assertThat(user.getMfaCode()).isNull();
            assertThat(user.getMfaExpiry()).isNull();
        }
    }





    /**
     * Test suite focusing on the forced credential migration/initialization
     * required immediately following the creation of a new administrative or resident account.
     */
    @Nested
    @DisplayName("changePasswordNewAccount()")
    class ChangePasswordNewAccountTests {

        @Test
        @DisplayName("Non-new account → AccessDeniedException")
        void alreadyInitialized_throws() {
            User user = activeUser();
            user.setIsNewAccount(false);
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));

            ChangePasswordRequest request = new ChangePasswordRequest(EMAIL, "New#Pass1", "New#Pass1");

            assertThatThrownBy(() -> authService.changePasswordNewAccount(request, IP))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Passwords don't match → BadCredentialsException")
        void passwordMismatch_throws() {
            User user = activeUser();
            user.setIsNewAccount(true);
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));

            ChangePasswordRequest request = new ChangePasswordRequest(EMAIL, "New#Pass1", "Different#Pass2");

            assertThatThrownBy(() -> authService.changePasswordNewAccount(request, IP))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("don't match");
        }

        @Test
        @DisplayName("Valid request → password saved, isNewAccount cleared, JWT returned")
        void valid_savesPasswordAndReturnsJwt() {
            User user = activeUser();
            user.setIsNewAccount(true);
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("New#Pass1")).thenReturn("hashed-new-pass");
            when(jwtService.generateToken(any(), any())).thenReturn(JWT_TOKEN);

            ChangePasswordRequest request = new ChangePasswordRequest(EMAIL, "New#Pass1", "New#Pass1");
            LoginResponse response = authService.changePasswordNewAccount(request, IP);

            assertThat(response.status()).isEqualTo("SUCCESS");
            assertThat(response.token()).isEqualTo(JWT_TOKEN);
            assertThat(user.getIsNewAccount()).isFalse();
            assertThat(user.getPassword()).isEqualTo("hashed-new-pass");
        }
    }



    /**
     * Test suite handling the dynamic generation and dispatchment of multi-factor codes
     * to verified secondary channels when primary messaging delivery fails or is inaccessible.
     */
    @Nested
    @DisplayName("sendBackupOtp()")
    class SendBackupOtpTests {

        @Test
        @DisplayName("No backup email configured → BusinessRuleException")
        void noBackupEmail_throws() {
            User user = activeUser();
            user.setSystemBackupEmail(null);
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.sendBackupOtp(EMAIL, IP))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("No backup email");
        }

        @Test
        @DisplayName("OTP still valid → reuses existing code without regenerating")
        void validOtpExists_reusesCode() {
            User user = activeUser();
            user.setSystemBackupEmail(BACKUP_EMAIL);
            user.setMfaCode(OTP_CODE);
            user.setMfaExpiry(LocalDateTime.now().plusMinutes(3)); // not expired
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));

            authService.sendBackupOtp(EMAIL, IP);

            // generateCode should NOT be called since the OTP is still valid
            verify(mfaService, never()).generateCode();
            verify(mfaService).sendMfaEmail(BACKUP_EMAIL, OTP_CODE);
        }

        @Test
        @DisplayName("OTP expired → generates new code and sends to backup email")
        void expiredOtp_generatesNewCode() {
            User user = activeUser();
            user.setSystemBackupEmail(BACKUP_EMAIL);
            user.setMfaCode(null); // no existing code
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));
            when(mfaService.generateCode()).thenReturn(OTP_CODE);

            authService.sendBackupOtp(EMAIL, IP);

            verify(mfaService).generateCode();
            verify(mfaService).sendMfaEmail(BACKUP_EMAIL, OTP_CODE);
        }
    }



    /**
     * Test suite handling the explicit expiration override and re-dispatchment
     * of standard multi-factor parameters.
     */
    @Nested
    @DisplayName("resendOtp()")
    class ResendOtpTests {

        @Test
        @DisplayName("Unknown email → UsernameNotFoundException")
        void unknownEmail_throws() {
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resendOtp(EMAIL, IP))
                    .isInstanceOf(UsernameNotFoundException.class);
        }

        @Test
        @DisplayName("Valid email → generates new OTP and sends to primary email")
        void valid_generatesAndSendsNewOtp() {
            User user = activeUser();
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));
            when(mfaService.generateCode()).thenReturn(OTP_CODE);

            authService.resendOtp(EMAIL, IP);

            verify(mfaService).generateCode();
            verify(mfaService).sendMfaEmail(EMAIL, OTP_CODE);
            assertThat(user.getMfaCode()).isEqualTo(OTP_CODE);
        }
    }


    /**
     * Test suite inspecting pre-binding verification constraints when an account
     * triggers secondary recovery channel initialization.
     */
    @Nested
    @DisplayName("initiateBackupEmailSetup()")
    class InitiateBackupEmailSetupTests {

        @Test
        @DisplayName("Blank backup email → IllegalArgumentException")
        void blankBackupEmail_throws() {
            User user = activeUser();
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.initiateBackupEmailSetup(EMAIL, "", IP))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be empty");
        }

        @Test
        @DisplayName("Invalid email format → IllegalArgumentException")
        void invalidFormat_throws() {
            User user = activeUser();
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.initiateBackupEmailSetup(EMAIL, "not-an-email", IP))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid email format");
        }

        @Test
        @DisplayName("Backup email same as primary → IllegalArgumentException")
        void sameAsPrimary_throws() {
            User user = activeUser();
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.initiateBackupEmailSetup(EMAIL, EMAIL, IP))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("same as your primary");
        }

        @Test
        @DisplayName("Valid backup email → OTP sent to backup email")
        void valid_sendsOtpToBackupEmail() {
            User user = activeUser();
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));
            when(mfaService.generateCode()).thenReturn(OTP_CODE);

            authService.initiateBackupEmailSetup(EMAIL, BACKUP_EMAIL, IP);

            verify(mfaService).sendMfaEmail(BACKUP_EMAIL, OTP_CODE);
        }
    }



    /**
     * Test suite handling the final commitment and transactional update
     * of secondary security bindings following successful token validation.
     */
    @Nested
    @DisplayName("verifyAndSaveBackupEmail()")
    class VerifyAndSaveBackupEmailTests {

        @Test
        @DisplayName("Invalid OTP → BadCredentialsException")
        void invalidOtp_throws() {
            User user = activeUser();
            user.setMfaCode("999999");
            user.setMfaExpiry(LocalDateTime.now().plusMinutes(5));
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));

            assertThatThrownBy(() ->
                    authService.verifyAndSaveBackupEmail(EMAIL, BACKUP_EMAIL, OTP_CODE, IP))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Invalid or expired");
        }

        @Test
        @DisplayName("Expired OTP → BadCredentialsException")
        void expiredOtp_throws() {
            User user = activeUser();
            user.setMfaCode(OTP_CODE);
            user.setMfaExpiry(LocalDateTime.now().minusMinutes(1));
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));

            assertThatThrownBy(() ->
                    authService.verifyAndSaveBackupEmail(EMAIL, BACKUP_EMAIL, OTP_CODE, IP))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("Valid OTP → backup email saved and OTP cleared")
        void valid_savesBackupEmailAndClearsOtp() {
            User user = activeUser();
            user.setMfaCode(OTP_CODE);
            user.setMfaExpiry(LocalDateTime.now().plusMinutes(5));
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));

            authService.verifyAndSaveBackupEmail(EMAIL, BACKUP_EMAIL, OTP_CODE, IP);

            assertThat(user.getSystemBackupEmail()).isEqualTo(BACKUP_EMAIL);
            assertThat(user.getMfaCode()).isNull();
            assertThat(user.getMfaExpiry()).isNull();
        }
    }


    /**
     * Test suite validating the multi-stage password recovery mechanism (`completePasswordReset`).
     * Guarantees that token destruction and cryptographically secure hashing
     * are strictly synchronized upon sequence completion.
     */
    @Nested
    @DisplayName("completePasswordReset()")
    class CompletePasswordResetTests {

        @Test
        @DisplayName("Invalid OTP → BadCredentialsException")
        void invalidOtp_throws() {
            User user = activeUser();
            user.setMfaCode("999999");
            user.setMfaExpiry(LocalDateTime.now().plusMinutes(5));
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));

            ResetPasswordRequest request = new ResetPasswordRequest(EMAIL, OTP_CODE, "New#Pass1", "New#Pass1");

            assertThatThrownBy(() -> authService.completePasswordReset(request, IP))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Verification failed");
        }

        @Test
        @DisplayName("Expired OTP → BadCredentialsException")
        void expiredOtp_throws() {
            User user = activeUser();
            user.setMfaCode(OTP_CODE);
            user.setMfaExpiry(LocalDateTime.now().minusMinutes(1));
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));

            ResetPasswordRequest request = new ResetPasswordRequest(EMAIL, OTP_CODE, "New#Pass1", "New#Pass1");

            assertThatThrownBy(() -> authService.completePasswordReset(request, IP))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Session expired");
        }

        @Test
        @DisplayName("Passwords don't match → IllegalArgumentException")
        void passwordMismatch_throws() {
            User user = activeUser();
            user.setMfaCode(OTP_CODE);
            user.setMfaExpiry(LocalDateTime.now().plusMinutes(5));
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));

            ResetPasswordRequest request = new ResetPasswordRequest(EMAIL, OTP_CODE, "New#Pass1", "Different#2");

            assertThatThrownBy(() -> authService.completePasswordReset(request, IP))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("do not match");
        }

        @Test
        @DisplayName("Valid reset → password saved, OTP cleared, isNewAccount false")
        void valid_savesPasswordAndClearsOtp() {
            User user = activeUser();
            user.setIsNewAccount(true);
            user.setMfaCode(OTP_CODE);
            user.setMfaExpiry(LocalDateTime.now().plusMinutes(5));
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("New#Pass1")).thenReturn("hashed-new-pass");

            ResetPasswordRequest request = new ResetPasswordRequest(EMAIL, OTP_CODE, "New#Pass1", "New#Pass1");
            authService.completePasswordReset(request, IP);

            assertThat(user.getPassword()).isEqualTo("hashed-new-pass");
            assertThat(user.getMfaCode()).isNull();
            assertThat(user.getIsNewAccount()).isFalse();
        }
    }



    /**
     * Test suite for early state checking (`verifyResetCode`).
     * Asserts that frontend orchestration can validate recovery keys
     * standalone prior to requesting final text updates.
     */
    @Nested
    @DisplayName("verifyResetCode()")
    class VerifyResetCodeTests {

        @Test
        @DisplayName("Invalid code → BadCredentialsException")
        void invalidCode_throws() {
            User user = activeUser();
            user.setMfaCode("999999");
            user.setMfaExpiry(LocalDateTime.now().plusMinutes(5));
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));

            VerifyCodeRequest request = new VerifyCodeRequest(EMAIL, OTP_CODE);

            assertThatThrownBy(() -> authService.verifyResetCode(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("invalid");
        }

        @Test
        @DisplayName("Expired code → BadCredentialsException")
        void expiredCode_throws() {
            User user = activeUser();
            user.setMfaCode(OTP_CODE);
            user.setMfaExpiry(LocalDateTime.now().minusMinutes(1));
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));

            VerifyCodeRequest request = new VerifyCodeRequest(EMAIL, OTP_CODE);

            assertThatThrownBy(() -> authService.verifyResetCode(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("Valid code → no exception thrown")
        void validCode_passes() {
            User user = activeUser();
            user.setMfaCode(OTP_CODE);
            user.setMfaExpiry(LocalDateTime.now().plusMinutes(5));
            when(userRepository.findBySystemEmail(EMAIL)).thenReturn(Optional.of(user));

            VerifyCodeRequest request = new VerifyCodeRequest(EMAIL, OTP_CODE);

            // Should complete without throwing
            authService.verifyResetCode(request);
        }
    }






}
