package com.barangay.people.officer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.people.officer.dto.LuponOfficerResponse;
import com.barangay.people.officer.dto.OfficerCaseHistoryResponse;
import com.barangay.people.officer.dto.OfficerCreateRequest;
import com.barangay.people.officer.dto.OfficerFilterRequest;
import com.barangay.people.officer.dto.OfficerStatsResponse;
import com.barangay.people.officer.dto.OfficerStatusUpdateRequest;
import com.barangay.people.officer.dto.OfficerTableResponse;
import com.barangay.people.officer.dto.OfficerUpdateRequest;
import com.barangay.people.officer.model.Officer;
import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.person.model.Person;
import com.barangay.people.person.repository.PersonRepository;
import com.barangay.usermanagement.department.model.Department;
import com.barangay.usermanagement.department.repository.DepartmentRepository;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.OfficerStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("OfficerService BDD Unit Tests")
class OfficerServiceTest {

    @Mock
    private OfficerRepository officerRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private CaseRepository caseRepository;

    @InjectMocks
    private OfficerService officerService;

    private Officer createTestOfficer(Long officerId, Long personId, String firstName, String lastName, Department dept, String position) {
        Person person = new Person();
        person.setId(personId);
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setMiddleName("Reyes");
        person.setEmail("officer@barangay.gov.ph");
        person.setContactNumber("09123456789");

        Officer officer = new Officer();
        officer.setId(officerId);
        officer.setPerson(person);
        officer.setDepartment(dept);
        officer.setPosition(position);
        officer.setIsGlobal(false);
        officer.setStatus(OfficerStatus.ACTIVE);
        officer.setActiveCasesCount(2);
        return officer;
    }

    @Nested
    @DisplayName("getOfficerStats Method Tests")
    class GetOfficerStatsTests {

        @Test
        @DisplayName("shouldReturnOfficerStats_whenCalled")
        void shouldReturnOfficerStats_whenCalled() {
            // Given
            when(officerRepository.countByStatusIn(anyCollection())).thenReturn(20L);
            when(officerRepository.countByStatus(OfficerStatus.ACTIVE)).thenReturn(18L);
            when(officerRepository.countByStatus(OfficerStatus.INACTIVE)).thenReturn(2L);
            when(departmentRepository.count()).thenReturn(5L);
            when(officerRepository.countByCreatedAtBeforeAndStatusIn(any(LocalDateTime.class), anyCollection())).thenReturn(15L);

            // When
            OfficerStatsResponse response = officerService.getOfficerStats();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.totalOfficers()).isEqualTo(20L);
            assertThat(response.activeOfficers()).isEqualTo(18L);
            assertThat(response.inactiveStaff()).isEqualTo(2L);
            assertThat(response.departmentsCount()).isEqualTo(5L);
            assertThat(response.totalOfficersTrend()).contains("% vs last month");
            verify(officerRepository, times(1)).countByStatusIn(anyCollection());
            verify(departmentRepository, times(1)).count();
        }
    }

    @Nested
    @DisplayName("getFilteredOfficersTable Method Tests")
    class GetFilteredOfficersTableTests {

        @Test
        @DisplayName("shouldReturnFilteredOfficers_whenFilterProvided")
        void shouldReturnFilteredOfficers_whenFilterProvided() {
            // Given
            Department dept = new Department();
            dept.setId(1L);
            dept.setName("VAWC");

            Officer officer = createTestOfficer(1L, 10L, "Ricardo", "Dalisay", dept, "Desk Officer");
            OfficerFilterRequest filter = new OfficerFilterRequest("Ricardo", "VAWC", OfficerStatus.ACTIVE);

            when(officerRepository.findFilteredOfficers("Ricardo", "VAWC", OfficerStatus.ACTIVE))
                    .thenReturn(List.of(officer));

            // When
            List<OfficerTableResponse> result = officerService.getFilteredOfficersTable(filter);

            // Then
            assertThat(result).isNotNull().hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(1L);
            assertThat(result.get(0).name()).isEqualTo("Ricardo Reyes Dalisay");
            assertThat(result.get(0).departmentName()).isEqualTo("VAWC");
            verify(officerRepository, times(1)).findFilteredOfficers("Ricardo", "VAWC", OfficerStatus.ACTIVE);
        }

        @Test
        @DisplayName("shouldReturnEmptyList_whenNoOfficersMatchFilter")
        void shouldReturnEmptyList_whenNoOfficersMatchFilter() {
            // Given
            OfficerFilterRequest filter = new OfficerFilterRequest("Unknown", null, null);
            when(officerRepository.findFilteredOfficers("Unknown", null, null))
                    .thenReturn(Collections.emptyList());

            // When
            List<OfficerTableResponse> result = officerService.getFilteredOfficersTable(filter);

            // Then
            assertThat(result).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("createOfficer Method Tests")
    class CreateOfficerTests {

        @Test
        @DisplayName("shouldCreateDepartmentOfficer_whenValidRequestProvided")
        void shouldCreateDepartmentOfficer_whenValidRequestProvided() {
            // Given
            Long personId = 10L;
            Long deptId = 2L;
            OfficerCreateRequest request = new OfficerCreateRequest(personId, deptId, false, "Investigator", OfficerStatus.ACTIVE);

            Person person = new Person();
            person.setId(personId);
            person.setFirstName("Juan");
            person.setLastName("Luna");

            Department dept = new Department();
            dept.setId(deptId);
            dept.setName("BCPC");

            Officer savedOfficer = createTestOfficer(5L, personId, "Juan", "Luna", dept, "Investigator");

            when(personRepository.findById(personId)).thenReturn(Optional.of(person));
            when(departmentRepository.findById(deptId)).thenReturn(Optional.of(dept));
            when(officerRepository.save(any(Officer.class))).thenReturn(savedOfficer);

            // When
            OfficerTableResponse response = officerService.createOfficer(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(5L);
            assertThat(response.name()).isEqualTo("Juan Reyes Luna");
            assertThat(response.departmentName()).isEqualTo("BCPC");
            assertThat(response.position()).isEqualTo("Investigator");
            verify(personRepository, times(1)).findById(personId);
            verify(officerRepository, times(1)).save(any(Officer.class));
        }

        @Test
        @DisplayName("shouldCreateGlobalAdminOfficer_whenIsGlobalIsTrue")
        void shouldCreateGlobalAdminOfficer_whenIsGlobalIsTrue() {
            // Given
            Long personId = 11L;
            OfficerCreateRequest request = new OfficerCreateRequest(personId, null, true, "Captain", OfficerStatus.ACTIVE);

            Person person = new Person();
            person.setId(personId);
            person.setFirstName("Kapitana");
            person.setLastName("Santos");

            Department adminDept = new Department();
            adminDept.setId(99L);
            adminDept.setName("ADMIN");

            Officer savedOfficer = createTestOfficer(6L, personId, "Kapitana", "Santos", adminDept, "Captain");
            savedOfficer.setIsGlobal(true);

            when(personRepository.findById(personId)).thenReturn(Optional.of(person));
            when(departmentRepository.findByName("ADMIN")).thenReturn(Optional.of(adminDept));
            when(officerRepository.save(any(Officer.class))).thenReturn(savedOfficer);

            // When
            OfficerTableResponse response = officerService.createOfficer(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isGlobal()).isTrue();
            assertThat(response.departmentName()).isEqualTo("OVERALL");
            verify(departmentRepository, times(1)).findByName("ADMIN");
        }

        @Test
        @DisplayName("shouldThrowIllegalArgumentException_whenPersonDoesNotExist")
        void shouldThrowIllegalArgumentException_whenPersonDoesNotExist() {
            // Given
            Long invalidPersonId = 999L;
            OfficerCreateRequest request = new OfficerCreateRequest(invalidPersonId, 1L, false, "Officer", OfficerStatus.ACTIVE);
            when(personRepository.findById(invalidPersonId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> officerService.createOfficer(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Selected resident/person not found with ID: " + invalidPersonId);
            verify(officerRepository, never()).save(any(Officer.class));
        }
    }

    @Nested
    @DisplayName("updateOfficer Method Tests")
    class UpdateOfficerTests {

        @Test
        @DisplayName("shouldUpdateOfficerDetails_whenValidRequestProvided")
        void shouldUpdateOfficerDetails_whenValidRequestProvided() {
            // Given
            Long officerId = 1L;
            Department currentDept = new Department();
            currentDept.setId(1L);
            currentDept.setName("VAWC");

            Department newDept = new Department();
            newDept.setId(2L);
            newDept.setName("LUPON");

            Officer officer = createTestOfficer(officerId, 10L, "Ricardo", "Dalisay", currentDept, "Officer I");
            OfficerUpdateRequest updateRequest = new OfficerUpdateRequest(2L, false, "Officer II", OfficerStatus.ACTIVE);

            when(officerRepository.findById(officerId)).thenReturn(Optional.of(officer));
            when(departmentRepository.findById(2L)).thenReturn(Optional.of(newDept));
            when(officerRepository.save(any(Officer.class))).thenReturn(officer);

            // When
            OfficerTableResponse response = officerService.updateOfficer(officerId, updateRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(officer.getPosition()).isEqualTo("Officer II");
            assertThat(officer.getDepartment().getName()).isEqualTo("LUPON");
            verify(officerRepository, times(1)).save(officer);
        }

        @Test
        @DisplayName("shouldThrowIllegalArgumentException_whenOfficerNotFound")
        void shouldThrowIllegalArgumentException_whenOfficerNotFound() {
            // Given
            Long invalidId = 999L;
            OfficerUpdateRequest updateRequest = new OfficerUpdateRequest(1L, false, "Chief", OfficerStatus.ACTIVE);
            when(officerRepository.findById(invalidId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> officerService.updateOfficer(invalidId, updateRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Officer not found with ID: " + invalidId);
        }
    }

    @Nested
    @DisplayName("updateOfficerStatus, delete, and restore Method Tests")
    class StatusAndLifecycleTests {

        @Test
        @DisplayName("shouldUpdateOfficerStatus_whenValidRequest")
        void shouldUpdateOfficerStatus_whenValidRequest() {
            // Given
            Long officerId = 1L;
            Officer officer = createTestOfficer(officerId, 10L, "Ricardo", "Dalisay", null, "Desk Officer");
            OfficerStatusUpdateRequest statusRequest = new OfficerStatusUpdateRequest(OfficerStatus.INACTIVE);

            when(officerRepository.findById(officerId)).thenReturn(Optional.of(officer));
            when(officerRepository.save(any(Officer.class))).thenReturn(officer);

            // When
            OfficerTableResponse response = officerService.updateOfficerStatus(officerId, statusRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(officer.getStatus()).isEqualTo(OfficerStatus.INACTIVE);
            verify(officerRepository, times(1)).save(officer);
        }

        @Test
        @DisplayName("shouldSoftDeleteOfficer_whenDeleted")
        void shouldSoftDeleteOfficer_whenDeleted() {
            // Given
            Long officerId = 1L;
            Officer officer = createTestOfficer(officerId, 10L, "Ricardo", "Dalisay", null, "Desk Officer");
            when(officerRepository.findById(officerId)).thenReturn(Optional.of(officer));

            // When
            officerService.deleteOfficer(officerId);

            // Then
            assertThat(officer.getIsDeleted()).isTrue();
            assertThat(officer.getStatus()).isEqualTo(OfficerStatus.INACTIVE);
            verify(officerRepository, times(1)).save(officer);
        }

        @Test
        @DisplayName("shouldRestoreOfficer_whenArchivedOrDeleted")
        void shouldRestoreOfficer_whenArchivedOrDeleted() {
            // Given
            Long officerId = 1L;
            Officer officer = createTestOfficer(officerId, 10L, "Ricardo", "Dalisay", null, "Desk Officer");
            officer.softDelete();
            when(officerRepository.findById(officerId)).thenReturn(Optional.of(officer));
            when(officerRepository.save(any(Officer.class))).thenReturn(officer);

            // When
            OfficerTableResponse response = officerService.restoreOfficer(officerId);

            // Then
            assertThat(response).isNotNull();
            assertThat(officer.getStatus()).isEqualTo(OfficerStatus.ACTIVE);
            assertThat(officer.getIsDeleted()).isFalse();
            verify(officerRepository, times(1)).save(officer);
        }
    }

    @Nested
    @DisplayName("getLuponOfficer and getOfficerCasesHandled Method Tests")
    class LuponAndCasesTests {

        @Test
        @DisplayName("shouldReturnLuponOfficers_whenCalled")
        void shouldReturnLuponOfficers_whenCalled() {
            // Given
            LuponOfficerResponse lupon = new LuponOfficerResponse(1L, "Ricardo", "Dalisay", "Reyes", "Lupon Member");
            when(officerRepository.findLuponOfficer()).thenReturn(List.of(lupon));

            // When
            List<LuponOfficerResponse> result = officerService.getLuponOfficer();

            // Then
            assertThat(result).isNotNull().hasSize(1);
            assertThat(result.get(0).getFullName()).contains("Ricardo");
            assertThat(result.get(0).position()).isEqualTo("Lupon Member");
            verify(officerRepository, times(1)).findLuponOfficer();
        }

        @Test
        @DisplayName("shouldReturnOfficerCasesHandled_whenOfficerHandledCases")
        void shouldReturnOfficerCasesHandled_whenOfficerHandledCases() {
            // Given
            Long officerId = 1L;
            Officer officer = createTestOfficer(officerId, 10L, "Ricardo", "Dalisay", null, "Desk Officer");

            Department blotterDept = new Department();
            blotterDept.setName("BLOTTER");

            Case blotterCase = new Case();
            blotterCase.setId(101L);
            blotterCase.setCaseNumber("BLOTTER-2026-001");
            blotterCase.setDepartment(blotterDept);
            blotterCase.setCaseStatus(CaseStatus.SETTLED);
            blotterCase.setCreatedAt(LocalDateTime.now());

            when(officerRepository.findById(officerId)).thenReturn(Optional.of(officer));
            when(caseRepository.findAllCasesHandledByOfficer(officerId, 10L)).thenReturn(List.of(blotterCase));

            // When
            List<OfficerCaseHistoryResponse> history = officerService.getOfficerCasesHandled(officerId);

            // Then
            assertThat(history).isNotNull().hasSize(1);
            assertThat(history.get(0).caseNumber()).isEqualTo("BLOTTER-2026-001");
            assertThat(history.get(0).department()).isEqualTo("BLOTTER");
            verify(caseRepository, times(1)).findAllCasesHandledByOfficer(officerId, 10L);
        }
    }
}
