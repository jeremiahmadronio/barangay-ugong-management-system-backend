package com.barangay.zshared.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService BDD Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;

    @Mock
    private UserDetails userDetails;

    private static final String TEST_SECRET_KEY = "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF";
    private static final long EXPIRATION_ONE_HOUR = 3600000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION_ONE_HOUR);
    }

    @Nested
    @DisplayName("Token Generation & Claims Extraction Tests")
    class TokenGenerationTests {

        @Test
        @DisplayName("Should generate token and extract correct username subject")
        void givenUserDetails_whenGenerateToken_thenExtractsSameUsername() {
            // Given
            given(userDetails.getUsername()).willReturn("admin@barangay.gov.ph");

            // When
            String token = jwtService.generateToken(new HashMap<>(), userDetails);
            String extractedUsername = jwtService.extractUsername(token);

            // Then
            assertThat(token).isNotNull().isNotBlank();
            assertThat(extractedUsername).isEqualTo("admin@barangay.gov.ph");
        }

        @Test
        @DisplayName("Should store and extract custom extra claims inside token")
        void givenExtraClaims_whenGenerateToken_thenExtractsCustomClaim() {
            // Given
            given(userDetails.getUsername()).willReturn("staff@barangay.gov.ph");
            Map<String, Object> claims = Map.of("role", "ROLE_STAFF", "department", "BLOTTER");

            // When
            String token = jwtService.generateToken(claims, userDetails);
            String role = jwtService.extractClaim(token, c -> c.get("role", String.class));
            String dept = jwtService.extractClaim(token, c -> c.get("department", String.class));

            // Then
            assertThat(role).isEqualTo("ROLE_STAFF");
            assertThat(dept).isEqualTo("BLOTTER");
        }
    }

    @Nested
    @DisplayName("Token Validity Tests")
    class TokenValidityTests {

        @Test
        @DisplayName("Should return true when token is valid and matches userDetails")
        void givenValidToken_whenIsTokenValid_thenReturnTrue() {
            // Given
            given(userDetails.getUsername()).willReturn("officer@barangay.gov.ph");
            String token = jwtService.generateToken(Collections.emptyMap(), userDetails);

            // When
            boolean valid = jwtService.isTokenValid(token, userDetails);

            // Then
            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("Should return false when token username does not match userDetails")
        void givenMismatchedUsername_whenIsTokenValid_thenReturnFalse() {
            // Given
            given(userDetails.getUsername()).willReturn("user1@barangay.gov.ph");
            String token = jwtService.generateToken(Collections.emptyMap(), userDetails);

            UserDetails otherUser = org.mockito.Mockito.mock(UserDetails.class);
            given(otherUser.getUsername()).willReturn("user2@barangay.gov.ph");

            // When
            boolean valid = jwtService.isTokenValid(token, otherUser);

            // Then
            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("Should return false when token has expired")
        void givenExpiredToken_whenIsTokenValid_thenThrowsOrReturnsFalse() {
            // Given: set expiration to negative value (already expired)
            ReflectionTestUtils.setField(jwtService, "jwtExpiration", -5000L);
            given(userDetails.getUsername()).willReturn("expired@barangay.gov.ph");
            String expiredToken = jwtService.generateToken(Collections.emptyMap(), userDetails);

            // When / Then: Expired token parsing triggers ExpiredJwtException or is invalid
            org.junit.jupiter.api.Assertions.assertThrows(io.jsonwebtoken.ExpiredJwtException.class, () -> {
                jwtService.isTokenValid(expiredToken, userDetails);
            });
        }
    }
}
