package com.barangay.usermanagement.department.service;

import com.barangay.usermanagement.department.dto.DepartmentResponse;
import com.barangay.usermanagement.department.dto.UserDepartmentResponse;
import com.barangay.usermanagement.department.model.Department;
import com.barangay.usermanagement.department.repository.DepartmentRepository;
import com.barangay.usermanagement.user.model.User;
import com.barangay.usermanagement.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DepartmentService departmentService;

    @Nested
    @DisplayName("getActiveDepartmentsForDropdown Method Tests")
    class GetActiveDepartmentsForDropdownTests {

        @Test
        @DisplayName("shouldReturnActiveDepartments_whenDepartmentsExist")
        void shouldReturnActiveDepartments_whenDepartmentsExist() {
            // Given
            Department dept1 = new Department(1L, "Barangay Affairs", "Office of Affairs", true);
            Department dept2 = new Department(2L, "Lupon Tagapamayapa", "Justice Panel", true);

            when(departmentRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(dept1, dept2));

            // When
            List<DepartmentResponse> result = departmentService.getActiveDepartmentsForDropdown();

            // Then
            assertThat(result).isNotNull().hasSize(2);
            assertThat(result.get(0).id()).isEqualTo(1L);
            assertThat(result.get(0).name()).isEqualTo("Barangay Affairs");
            assertThat(result.get(1).id()).isEqualTo(2L);
            assertThat(result.get(1).name()).isEqualTo("Lupon Tagapamayapa");

            verify(departmentRepository, times(1)).findByIsActiveTrueOrderByNameAsc();
        }

        @Test
        @DisplayName("shouldReturnEmptyList_whenNoActiveDepartmentsFound")
        void shouldReturnEmptyList_whenNoActiveDepartmentsFound() {
            // Given
            when(departmentRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(Collections.emptyList());

            // When
            List<DepartmentResponse> result = departmentService.getActiveDepartmentsForDropdown();

            // Then
            assertThat(result).isNotNull().isEmpty();
            verify(departmentRepository, times(1)).findByIsActiveTrueOrderByNameAsc();
        }
    }

    @Nested
    @DisplayName("getUserDepartments Method Tests")
    class GetUserDepartmentsTests {

        @Test
        @DisplayName("shouldReturnUserDepartments_whenUserExistsWithAssignedDepartments")
        void shouldReturnUserDepartments_whenUserExistsWithAssignedDepartments() {
            // Given
            Long userId = 10L;
            Department dept = new Department(1L, "VAWC Desk", "Women & Children Protection", true);

            User user = new User();
            user.setId(userId);
            user.setDepartments(Set.of(dept));

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            // When
            UserDepartmentResponse response = departmentService.getUserDepartments(userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.departmentIds()).containsExactly(1L);
            assertThat(response.departments()).hasSize(1);
            assertThat(response.departments().get(0).name()).isEqualTo("VAWC Desk");

            verify(userRepository, times(1)).findById(userId);
        }

        @Test
        @DisplayName("shouldReturnEmptyDepartmentLists_whenUserExistsWithoutDepartments")
        void shouldReturnEmptyDepartmentLists_whenUserExistsWithoutDepartments() {
            // Given
            Long userId = 12L;
            User user = new User();
            user.setId(userId);
            user.setDepartments(Collections.emptySet());

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            // When
            UserDepartmentResponse response = departmentService.getUserDepartments(userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.departmentIds()).isEmpty();
            assertThat(response.departments()).isEmpty();

            verify(userRepository, times(1)).findById(userId);
        }

        @Test
        @DisplayName("shouldThrowIllegalArgumentException_whenUserNotFound")
        void shouldThrowIllegalArgumentException_whenUserNotFound() {
            // Given
            Long userId = 999L;
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> departmentService.getUserDepartments(userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User not found with id: " + userId);

            verify(userRepository, times(1)).findById(userId);
        }
    }
}
