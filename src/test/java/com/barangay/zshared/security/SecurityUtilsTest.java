package com.barangay.zshared.security;

import com.barangay.usermanagement.permission.model.Permission;
import com.barangay.usermanagement.role.model.Role;
import com.barangay.usermanagement.user.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SecurityUtils BDD Unit Tests")
class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser(User user) {
        CustomUserDetails details = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Nested
    @DisplayName("getCurrentUser Tests")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Should return null when unauthenticated / empty context")
        void givenUnauthenticatedContext_whenGetCurrentUser_thenReturnNull() {
            // When
            User result = SecurityUtils.getCurrentUser();

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should return authenticated User when principal is CustomUserDetails")
        void givenAuthenticatedUser_whenGetCurrentUser_thenReturnUser() {
            // Given
            User user = new User();
            user.setId(99L);
            user.setSystemEmail("admin@barangay.gov.ph");
            Role role = new Role();
            role.setRoleName("ADMIN");
            user.setRole(role);

            authenticateUser(user);

            // When
            User result = SecurityUtils.getCurrentUser();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(99L);
            assertThat(result.getSystemEmail()).isEqualTo("admin@barangay.gov.ph");
        }
    }

    @Nested
    @DisplayName("hasPermission Tests")
    class HasPermissionTests {

        @Test
        @DisplayName("Should return false when user is not logged in")
        void givenNoUser_whenHasPermission_thenReturnFalse() {
            // When
            boolean hasPerm = SecurityUtils.hasPermission("View Cases");

            // Then
            assertThat(hasPerm).isFalse();
        }

        @Test
        @DisplayName("Should return true for ROOT_ADMIN for any permission")
        void givenRootAdmin_whenHasPermission_thenReturnTrue() {
            // Given
            User user = new User();
            Role role = new Role();
            role.setRoleName("ROOT_ADMIN");
            user.setRole(role);
            authenticateUser(user);

            // When
            boolean hasPerm = SecurityUtils.hasPermission("Delete Case");

            // Then
            assertThat(hasPerm).isTrue();
        }

        @Test
        @DisplayName("Should return true for SYSTEM_ADMIN for any permission")
        void givenSystemAdmin_whenHasPermission_thenReturnTrue() {
            // Given
            User user = new User();
            Role role = new Role();
            role.setRoleName("SYSTEM_ADMIN");
            user.setRole(role);
            authenticateUser(user);

            // When
            boolean hasPerm = SecurityUtils.hasPermission("Manage Conciliation");

            // Then
            assertThat(hasPerm).isTrue();
        }

        @Test
        @DisplayName("Should return true when staff user has the matching custom permission")
        void givenStaffWithMatchingPermission_whenHasPermission_thenReturnTrue() {
            // Given
            User user = new User();
            Role role = new Role();
            role.setRoleName("STAFF");
            user.setRole(role);

            Permission perm = new Permission();
            perm.setPermissionName("File Complaint");
            user.setCustomPermissions(Set.of(perm));

            authenticateUser(user);

            // When
            boolean hasPerm = SecurityUtils.hasPermission("File Complaint");

            // Then
            assertThat(hasPerm).isTrue();
        }

        @Test
        @DisplayName("Should return false when staff user lacks the requested permission")
        void givenStaffWithoutPermission_whenHasPermission_thenReturnFalse() {
            // Given
            User user = new User();
            Role role = new Role();
            role.setRoleName("STAFF");
            user.setRole(role);

            Permission perm = new Permission();
            perm.setPermissionName("View Cases");
            user.setCustomPermissions(Set.of(perm));

            authenticateUser(user);

            // When
            boolean hasPerm = SecurityUtils.hasPermission("Permanent Delete Case");

            // Then
            assertThat(hasPerm).isFalse();
        }
    }
}
