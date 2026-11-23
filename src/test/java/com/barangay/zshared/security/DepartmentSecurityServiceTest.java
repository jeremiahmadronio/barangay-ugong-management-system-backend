package com.barangay.zshared.security;

import com.barangay.usermanagement.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("DepartmentSecurityService Unit Tests")
class DepartmentSecurityServiceTest {

    @InjectMocks
    private DepartmentSecurityService departmentSecurityService;

    @Mock
    private Authentication authentication;

    @Nested
    @DisplayName("isInDept Tests")
    class IsInDeptTests {

        @Test
        @DisplayName("Should return true when user has matching department authority")
        void givenUserWithMatchingDept_whenIsInDept_thenReturnTrue() {
            // Given
            given(authentication.getAuthorities()).willAnswer(inv ->
                    List.of(new SimpleGrantedAuthority("DEPT_BLOTTER"))
            );

            // When
            boolean result = departmentSecurityService.isInDept(authentication, "BLOTTER");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when user belongs to different department")
        void givenUserWithDifferentDept_whenIsInDept_thenReturnFalse() {
            // Given
            given(authentication.getAuthorities()).willAnswer(inv ->
                    List.of(new SimpleGrantedAuthority("DEPT_VAWC"))
            );

            // When
            boolean result = departmentSecurityService.isInDept(authentication, "BLOTTER");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should handle lowercase department parameter case-insensitively")
        void givenLowercaseDeptParam_whenIsInDept_thenReturnTrue() {
            // Given
            given(authentication.getAuthorities()).willAnswer(inv ->
                    List.of(new SimpleGrantedAuthority("DEPT_LUPON"))
            );

            // When
            boolean result = departmentSecurityService.isInDept(authentication, "lupon");

            // Then
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("canAccessDept Tests")
    class CanAccessDeptTests {

        @Test
        @DisplayName("Should return true when user is ROOT_ADMIN regardless of department")
        void givenRootAdmin_whenCanAccessDept_thenReturnTrue() {
            // Given
            given(authentication.getAuthorities()).willAnswer(inv ->
                    List.of(new SimpleGrantedAuthority("ROLE_ROOT_ADMIN"))
            );

            // When
            boolean result = departmentSecurityService.canAccessDept(authentication, "VAWC");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return true when user is SYSTEM_ADMIN regardless of department")
        void givenSystemAdmin_whenCanAccessDept_thenReturnTrue() {
            // Given
            given(authentication.getAuthorities()).willAnswer(inv ->
                    List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"))
            );

            // When
            boolean result = departmentSecurityService.canAccessDept(authentication, "BCPC");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return true when user is staff with matching department authority")
        void givenMatchingDeptStaff_whenCanAccessDept_thenReturnTrue() {
            // Given
            given(authentication.getAuthorities()).willAnswer(inv ->
                    List.of(new SimpleGrantedAuthority("ROLE_STAFF"), new SimpleGrantedAuthority("DEPT_CLEARANCE"))
            );

            // When
            boolean result = departmentSecurityService.canAccessDept(authentication, "CLEARANCE");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when non-admin user does not belong to target department")
        void givenNonAdminOtherDept_whenCanAccessDept_thenReturnFalse() {
            // Given
            given(authentication.getAuthorities()).willAnswer(inv ->
                    List.of(new SimpleGrantedAuthority("ROLE_STAFF"), new SimpleGrantedAuthority("DEPT_BLOTTER"))
            );

            // When
            boolean result = departmentSecurityService.canAccessDept(authentication, "VAWC");

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("isOwner Tests")
    class IsOwnerTests {

        @Test
        @DisplayName("Should return true when principal ID matches resource owner ID")
        void givenMatchingUser_whenIsOwner_thenReturnTrue() {
            // Given
            User user = new User();
            user.setId(42L);
            CustomUserDetails principal = new CustomUserDetails(user);
            given(authentication.getPrincipal()).willReturn(principal);

            // When
            boolean result = departmentSecurityService.isOwner(authentication, 42L);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when principal ID differs from resource owner ID")
        void givenDifferentUser_whenIsOwner_thenReturnFalse() {
            // Given
            User user = new User();
            user.setId(42L);
            CustomUserDetails principal = new CustomUserDetails(user);
            given(authentication.getPrincipal()).willReturn(principal);

            // When
            boolean result = departmentSecurityService.isOwner(authentication, 99L);

            // Then
            assertThat(result).isFalse();
        }
    }
}
