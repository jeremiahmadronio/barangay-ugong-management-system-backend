package com.barangay.usermanagement.permission.service;

import com.barangay.usermanagement.permission.dto.PermissionResponse;
import com.barangay.usermanagement.permission.model.Permission;
import com.barangay.usermanagement.permission.repository.PermissionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private PermissionService permissionService;

    @Nested
    @DisplayName("getAllPermissions Method Tests")
    class GetAllPermissionsTests {

        @Test
        @DisplayName("shouldReturnAllPermissions_whenPermissionsExist")
        void shouldReturnAllPermissions_whenPermissionsExist() {
            // Given
            Permission perm1 = new Permission();
            perm1.setId(1L);
            perm1.setPermissionName("File Complaint");
            perm1.setModule("BLOTTER");

            Permission perm2 = new Permission();
            perm2.setId(2L);
            perm2.setPermissionName("View Cases");
            perm2.setModule("BLOTTER");

            when(permissionRepository.findAllByOrderByModuleAscPermissionNameAsc()).thenReturn(List.of(perm1, perm2));

            // When
            List<PermissionResponse> result = permissionService.getAllPermissions();

            // Then
            assertThat(result).isNotNull().hasSize(2);
            assertThat(result.get(0).permissionName()).isEqualTo("File Complaint");
            assertThat(result.get(1).permissionName()).isEqualTo("View Cases");

            verify(permissionRepository, times(1)).findAllByOrderByModuleAscPermissionNameAsc();
        }

        @Test
        @DisplayName("shouldReturnEmptyList_whenNoPermissionsExist")
        void shouldReturnEmptyList_whenNoPermissionsExist() {
            // Given
            when(permissionRepository.findAllByOrderByModuleAscPermissionNameAsc()).thenReturn(Collections.emptyList());

            // When
            List<PermissionResponse> result = permissionService.getAllPermissions();

            // Then
            assertThat(result).isNotNull().isEmpty();
            verify(permissionRepository, times(1)).findAllByOrderByModuleAscPermissionNameAsc();
        }
    }

    @Nested
    @DisplayName("getPermissionsByDepartment Method Tests")
    class GetPermissionsByDepartmentTests {

        @Test
        @DisplayName("shouldReturnDepartmentPermissions_whenValidDepartmentProvided")
        void shouldReturnDepartmentPermissions_whenValidDepartmentProvided() {
            // Given
            String department = "VAWC";
            Permission perm = new Permission();
            perm.setId(5L);
            perm.setPermissionName("File Complaint");
            perm.setModule("VAWC");

            when(permissionRepository.findAll()).thenReturn(List.of(perm));

            // When
            List<PermissionResponse> result = permissionService.getPermissionsByDepartment(department);

            // Then
            assertThat(result).isNotNull().isNotEmpty();
            assertThat(result.stream().anyMatch(p -> p.permissionName().equals("File Complaint"))).isTrue();
        }

        @Test
        @DisplayName("shouldReturnEmptyList_whenKapitanaDepartmentProvided")
        void shouldReturnEmptyList_whenKapitanaDepartmentProvided() {
            // Given
            String department = "OFFICE OF THE BARANGAY CAPTAIN";
            when(permissionRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<PermissionResponse> result = permissionService.getPermissionsByDepartment(department);

            // Then
            assertThat(result).isNotNull().isEmpty();
            verify(permissionRepository, never()).findAllByOrderByModuleAscPermissionNameAsc();
        }

        @Test
        @DisplayName("shouldReturnEmptyList_whenDepartmentNameIsNullOrBlank")
        void shouldReturnEmptyList_whenDepartmentNameIsNullOrBlank() {
            // Given
            String nullDept = null;
            String blankDept = "   ";

            // When
            List<PermissionResponse> resultNull = permissionService.getPermissionsByDepartment(nullDept);
            List<PermissionResponse> resultBlank = permissionService.getPermissionsByDepartment(blankDept);

            // Then
            assertThat(resultNull).isNotNull().isEmpty();
            assertThat(resultBlank).isNotNull().isEmpty();
            verify(permissionRepository, never()).findAll();
        }
    }
}
