package com.barangay.people.resident.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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

import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.clearance.repository.IssuedCertificateRepository;
import com.barangay.ftjs.repository.FirstTimeJobSeekerRepository;
import com.barangay.people.person.model.Person;
import com.barangay.people.person.repository.PersonRepository;
import com.barangay.people.person.repository.RelationshipRepository;
import com.barangay.people.resident.dto.ResidentCreateRequest;
import com.barangay.people.resident.dto.ResidentDetailResponse;
import com.barangay.people.resident.dto.ResidentFilterRequest;
import com.barangay.people.resident.dto.ResidentManagementStatsResponse;
import com.barangay.people.resident.dto.ResidentRelativeResponse;
import com.barangay.people.resident.dto.ResidentSearchOptionResponse;
import com.barangay.people.resident.dto.ResidentStatusUpdateRequest;
import com.barangay.people.resident.dto.ResidentSuggestionsResponse;
import com.barangay.people.resident.dto.ResidentTableResponse;
import com.barangay.people.resident.dto.ResidentUpdateRequest;
import com.barangay.people.resident.model.Resident;
import com.barangay.people.resident.repository.ResidentDocumentRepository;
import com.barangay.people.resident.repository.ResidentRepository;
import com.barangay.zshared.enumerated.ResidentStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResidentService BDD Unit Tests")
class ResidentServiceTest {

    @Mock
    private ResidentRepository residentRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private RelationshipRepository relationshipRepository;

    @Mock
    private ResidentDocumentRepository residentDocumentRepository;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private IssuedCertificateRepository issuedCertificateRepository;

    @Mock
    private FirstTimeJobSeekerRepository ftjsRepository;

    @InjectMocks
    private ResidentService residentService;

    private Resident createTestResident(Long residentId, Long personId, String first, String last) {
        Person person = new Person();
        person.setId(personId);
        person.setFirstName(first);
        person.setLastName(last);
        person.setMiddleName("Cruz");
        person.setGender("Male");
        person.setBirthDate(LocalDate.now().minusYears(25));
        person.setCompleteAddress("Zone 1, Barangay");
        person.setContactNumber("09123456789");
        person.setIsResident(true);

        Resident resident = new Resident();
        resident.setResidentId(residentId);
        resident.setPerson(person);
        resident.setBarangayIdNumber("BRGY-2026-0001");
        resident.setHouseholdNumber("HH-001");
        resident.setPrecinctNumber("PR-10A");
        resident.setStatus(ResidentStatus.ACTIVE);
        resident.setIsHeadOfFamily(true);
        resident.setCitizenship("Filipino");
        resident.setDateOfResidency(LocalDate.of(2020, 1, 1));
        return resident;
    }

    @Nested
    @DisplayName("getResidentStats Method Tests")
    class GetResidentStatsTests {

        @Test
        @DisplayName("shouldReturnAccurateResidentStats_whenCalled")
        void shouldReturnAccurateResidentStats_whenCalled() {
            // Given
            when(residentRepository.countByStatusIn(anyCollection())).thenReturn(100L);
            when(residentRepository.countByStatusInAndIsHeadOfFamilyTrue(anyCollection())).thenReturn(30L);
            when(residentRepository.countSeniorCitizensByStatuses(any(LocalDate.class), anyCollection())).thenReturn(15L);
            when(residentRepository.countByStatusInAndCreatedAtBetween(anyCollection(), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(10L, 80L, 8L);
            when(residentRepository.countByStatusInAndIsHeadOfFamilyTrueAndCreatedAtBetween(anyCollection(), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(25L);
            when(residentRepository.countSeniorCitizensByStatusesBetween(any(LocalDate.class), anyCollection(), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(12L);

            // When
            ResidentManagementStatsResponse response = residentService.getResidentStats();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.totalResidents().count()).isEqualTo(100L);
            assertThat(response.headsOfFamily().count()).isEqualTo(30L);
            assertThat(response.seniorCitizens().count()).isEqualTo(15L);
            assertThat(response.residentsThisMonth().count()).isEqualTo(10L);
            verify(residentRepository, times(1)).countByStatusIn(anyCollection());
        }
    }

    @Nested
    @DisplayName("getFilteredResidentsTable Method Tests")
    class GetFilteredResidentsTableTests {

        @Test
        @DisplayName("shouldReturnFilteredResidentsTable_whenFilterCriteriaProvided")
        void shouldReturnFilteredResidentsTable_whenFilterCriteriaProvided() {
            // Given
            ResidentFilterRequest filter = new ResidentFilterRequest("Juan", "ACTIVE", "Male", "ADULT");
            Resident r1 = createTestResident(1L, 10L, "Juan", "Dela Cruz");
            when(residentRepository.findFilteredResidents(anyString(), eq(ResidentStatus.ACTIVE), eq("Male")))
                    .thenReturn(List.of(r1));

            // When
            List<ResidentTableResponse> result = residentService.getFilteredResidentsTable(filter);

            // Then
            assertThat(result).isNotNull().hasSize(1);
            assertThat(result.get(0).fullName()).isEqualTo("Juan Cruz Dela Cruz");
            assertThat(result.get(0).barangayIdNumber()).isEqualTo("BRGY-2026-0001");
            verify(residentRepository, times(1)).findFilteredResidents(anyString(), eq(ResidentStatus.ACTIVE), eq("Male"));
        }

        @Test
        @DisplayName("shouldReturnEmptyList_whenNoResidentsMatchFilter")
        void shouldReturnEmptyList_whenNoResidentsMatchFilter() {
            // Given
            ResidentFilterRequest filter = new ResidentFilterRequest("NonExistent", "ACTIVE", null, "ALL");
            when(residentRepository.findFilteredResidents(anyString(), eq(ResidentStatus.ACTIVE), eq(null)))
                    .thenReturn(Collections.emptyList());

            // When
            List<ResidentTableResponse> result = residentService.getFilteredResidentsTable(filter);

            // Then
            assertThat(result).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("getResidentDetail Method Tests")
    class GetResidentDetailTests {

        @Test
        @DisplayName("shouldReturnResidentDetail_whenResidentExists")
        void shouldReturnResidentDetail_whenResidentExists() {
            // Given
            Long residentId = 1L;
            Resident resident = createTestResident(residentId, 10L, "Juan", "Dela Cruz");

            when(residentRepository.findById(residentId)).thenReturn(Optional.of(resident));
            when(relationshipRepository.findByPersonId(10L)).thenReturn(Collections.emptyList());
            when(residentDocumentRepository.findByResidentResidentId(residentId)).thenReturn(Collections.emptyList());
            when(caseRepository.findAllHistoricalCasesByPersonInCaseRoles(eq(10L), anyString(), anyString())).thenReturn(Collections.emptyList());
            when(issuedCertificateRepository.findAllCertificatesByPersonOrName(eq(10L), anyString())).thenReturn(Collections.emptyList());
            when(ftjsRepository.findAllHistoricalByResidentOrPerson(eq(residentId), eq(10L))).thenReturn(Collections.emptyList());

            // When
            ResidentDetailResponse response = residentService.getResidentDetail(residentId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(residentId);
            assertThat(response.barangayIdNumber()).isEqualTo("BRGY-2026-0001");
            assertThat(response.personInfo().fullName()).isEqualTo("Juan Cruz Dela Cruz");
            verify(residentRepository, times(1)).findById(residentId);
        }

        @Test
        @DisplayName("shouldThrowRuntimeException_whenResidentNotFound")
        void shouldThrowRuntimeException_whenResidentNotFound() {
            // Given
            Long residentId = 999L;
            when(residentRepository.findById(residentId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> residentService.getResidentDetail(residentId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Resident not found with id: " + residentId);
            verify(residentRepository, times(1)).findById(residentId);
        }
    }

    @Nested
    @DisplayName("createResident Method Tests")
    class CreateResidentTests {

        @Test
        @DisplayName("shouldCreateAndReturnResident_whenValidRequestProvided")
        void shouldCreateAndReturnResident_whenValidRequestProvided() {
            // Given
            ResidentCreateRequest request = new ResidentCreateRequest(
                    "Maria", "Santos", "Lopez", null,
                    "09987654321", "maria@test.com", "Zone 2, Barangay", "Female",
                    "Single", "Accountant", LocalDate.of(1995, 5, 20), (short) 30,
                    "BRGY-2026-0002", "HH-002", "PR-05B", false,
                    "College Graduate", "Filipino", LocalDate.of(2021, 6, 1),
                    ResidentStatus.ACTIVE, null, null, null
            );

            Person savedPerson = new Person();
            savedPerson.setId(20L);
            savedPerson.setFirstName("Maria");
            savedPerson.setLastName("Santos");
            savedPerson.setMiddleName("Lopez");
            savedPerson.setIsResident(true);

            Resident savedResident = new Resident();
            savedResident.setResidentId(2L);
            savedResident.setPerson(savedPerson);
            savedResident.setBarangayIdNumber("BRGY-2026-0002");
            savedResident.setStatus(ResidentStatus.ACTIVE);

            when(personRepository.save(any(Person.class))).thenReturn(savedPerson);
            when(residentRepository.save(any(Resident.class))).thenReturn(savedResident);

            // When
            ResidentTableResponse response = residentService.createResident(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(2L);
            assertThat(response.fullName()).isEqualTo("Maria Lopez Santos");
            assertThat(response.barangayIdNumber()).isEqualTo("BRGY-2026-0002");
            verify(personRepository, times(1)).save(any(Person.class));
            verify(residentRepository, times(1)).save(any(Resident.class));
        }
    }

    @Nested
    @DisplayName("updateResident Method Tests")
    class UpdateResidentTests {

        @Test
        @DisplayName("shouldUpdateAndReturnResident_whenValidRequestProvided")
        void shouldUpdateAndReturnResident_whenValidRequestProvided() {
            // Given
            Long residentId = 1L;
            Resident resident = createTestResident(residentId, 10L, "Juan", "Dela Cruz");
            ResidentUpdateRequest updateRequest = new ResidentUpdateRequest(
                    "Juanito", "Dela Cruz", "Cruz", "Jr.",
                    "09112223333", "juanito@test.com", "Zone 3, Barangay", "Male",
                    "Married", "Developer", LocalDate.of(1990, 1, 1), (short) 35,
                    "BRGY-2026-0001", "HH-001", "PR-10A", true,
                    "College Graduate", "Filipino", LocalDate.of(2020, 1, 1),
                    ResidentStatus.ACTIVE, "Updated profile", null, null, null
            );

            when(residentRepository.findById(residentId)).thenReturn(Optional.of(resident));
            when(personRepository.save(any(Person.class))).thenReturn(resident.getPerson());
            when(residentRepository.save(any(Resident.class))).thenReturn(resident);

            // When
            ResidentTableResponse response = residentService.updateResident(residentId, updateRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(residentId);
            verify(residentRepository, times(1)).findById(residentId);
            verify(personRepository, times(1)).save(any(Person.class));
            verify(residentRepository, times(1)).save(resident);
        }

        @Test
        @DisplayName("shouldThrowRuntimeException_whenUpdatingNonExistentResident")
        void shouldThrowRuntimeException_whenUpdatingNonExistentResident() {
            // Given
            Long residentId = 999L;
            ResidentUpdateRequest updateRequest = new ResidentUpdateRequest(
                    "Juan", "Cruz", null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null
            );
            when(residentRepository.findById(residentId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> residentService.updateResident(residentId, updateRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Resident not found with id: " + residentId);
            verify(personRepository, never()).save(any(Person.class));
        }
    }

    @Nested
    @DisplayName("updateResidentStatus Method Tests")
    class UpdateResidentStatusTests {

        @Test
        @DisplayName("shouldUpdateStatusSuccessfully_whenValidRequest")
        void shouldUpdateStatusSuccessfully_whenValidRequest() {
            // Given
            Long residentId = 1L;
            Resident resident = createTestResident(residentId, 10L, "Juan", "Dela Cruz");
            ResidentStatusUpdateRequest statusRequest = new ResidentStatusUpdateRequest(ResidentStatus.INACTIVE, "Relocated");

            when(residentRepository.findById(residentId)).thenReturn(Optional.of(resident));
            when(residentRepository.save(any(Resident.class))).thenReturn(resident);

            // When
            ResidentTableResponse response = residentService.updateResidentStatus(residentId, statusRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(resident.getStatus()).isEqualTo(ResidentStatus.INACTIVE);
            assertThat(resident.getStatusRemarks()).isEqualTo("Relocated");
            verify(residentRepository, times(1)).save(resident);
        }
    }

    @Nested
    @DisplayName("archiveResident and restoreResident Method Tests")
    class ArchiveAndRestoreTests {

        @Test
        @DisplayName("shouldArchiveResidentSuccessfully_whenResidentExists")
        void shouldArchiveResidentSuccessfully_whenResidentExists() {
            // Given
            Long residentId = 1L;
            Resident resident = createTestResident(residentId, 10L, "Juan", "Dela Cruz");
            when(residentRepository.findById(residentId)).thenReturn(Optional.of(resident));
            when(residentRepository.save(any(Resident.class))).thenReturn(resident);

            // When
            ResidentTableResponse response = residentService.archiveResident(residentId);

            // Then
            assertThat(response).isNotNull();
            assertThat(resident.getStatus()).isEqualTo(ResidentStatus.ARCHIVED);
            verify(residentRepository, times(1)).save(resident);
        }

        @Test
        @DisplayName("shouldRestoreResidentSuccessfully_whenResidentIsArchived")
        void shouldRestoreResidentSuccessfully_whenResidentIsArchived() {
            // Given
            Long residentId = 1L;
            Resident resident = createTestResident(residentId, 10L, "Juan", "Dela Cruz");
            resident.setStatus(ResidentStatus.ARCHIVED);
            when(residentRepository.findById(residentId)).thenReturn(Optional.of(resident));
            when(residentRepository.save(any(Resident.class))).thenReturn(resident);

            // When
            ResidentTableResponse response = residentService.restoreResident(residentId);

            // Then
            assertThat(response).isNotNull();
            assertThat(resident.getStatus()).isEqualTo(ResidentStatus.ACTIVE);
            verify(residentRepository, times(1)).save(resident);
        }
    }

    @Nested
    @DisplayName("deleteResident Method Tests")
    class DeleteResidentTests {

        @Test
        @DisplayName("shouldSoftDeleteResidentSuccessfully_whenResidentExists")
        void shouldSoftDeleteResidentSuccessfully_whenResidentExists() {
            // Given
            Long residentId = 1L;
            Resident resident = createTestResident(residentId, 10L, "Juan", "Dela Cruz");
            when(residentRepository.findById(residentId)).thenReturn(Optional.of(resident));

            // When
            residentService.deleteResident(residentId);

            // Then
            assertThat(resident.getStatus()).isEqualTo(ResidentStatus.INACTIVE);
            assertThat(resident.getIsDeleted()).isTrue();
            verify(residentRepository, times(1)).save(resident);
        }
    }

    @Nested
    @DisplayName("getSuggestions Method Tests")
    class GetSuggestionsTests {

        @Test
        @DisplayName("shouldReturnNextSuggestedBarangayIdAndHousehold_whenDataExists")
        void shouldReturnNextSuggestedBarangayIdAndHousehold_whenDataExists() {
            // Given
            Resident latestResident = createTestResident(15L, 100L, "Pedro", "Penduko");
            when(residentRepository.findLatestResident()).thenReturn(Optional.of(latestResident));
            when(residentRepository.findLatestHouseholdNumber()).thenReturn(Optional.of("HH-015"));

            // When
            ResidentSuggestionsResponse suggestions = residentService.getSuggestions();

            // Then
            assertThat(suggestions).isNotNull();
            assertThat(suggestions.suggestedBarangayId()).contains("0016");
            assertThat(suggestions.suggestedHouseholdNumber()).isEqualTo("HH-015");
        }
    }

    @Nested
    @DisplayName("searchActiveResidents & getRelativesByLastName Method Tests")
    class SearchAndRelativesTests {

        @Test
        @DisplayName("shouldReturnMatchingActiveResidents_whenQueryProvided")
        void shouldReturnMatchingActiveResidents_whenQueryProvided() {
            // Given
            String search = "Juan";
            Resident resident = createTestResident(1L, 10L, "Juan", "Dela Cruz");
            when(residentRepository.searchActiveResidents(search)).thenReturn(List.of(resident));

            // When
            List<ResidentSearchOptionResponse> result = residentService.searchActiveResidents(search);

            // Then
            assertThat(result).isNotNull().hasSize(1);
            assertThat(result.get(0).fullName()).isEqualTo("Juan Cruz Dela Cruz");
            verify(residentRepository, times(1)).searchActiveResidents(search);
        }

        @Test
        @DisplayName("shouldReturnRelativesByLastName_whenValidLastNameProvided")
        void shouldReturnRelativesByLastName_whenValidLastNameProvided() {
            // Given
            String lastName = "Dela Cruz";
            Resident resident = createTestResident(1L, 10L, "Juan", "Dela Cruz");
            when(residentRepository.findActiveResidentsByLastName(lastName)).thenReturn(List.of(resident));

            // When
            List<ResidentRelativeResponse> result = residentService.getRelativesByLastName(lastName);

            // Then
            assertThat(result).isNotNull().hasSize(1);
            assertThat(result.get(0).fullName()).isEqualTo("Juan Cruz Dela Cruz");
            verify(residentRepository, times(1)).findActiveResidentsByLastName(lastName);
        }

        @Test
        @DisplayName("shouldReturnEmptyList_whenLastNameIsBlankOrNull")
        void shouldReturnEmptyList_whenLastNameIsBlankOrNull() {
            // When
            List<ResidentRelativeResponse> resultNull = residentService.getRelativesByLastName(null);
            List<ResidentRelativeResponse> resultBlank = residentService.getRelativesByLastName("   ");

            // Then
            assertThat(resultNull).isNotNull().isEmpty();
            assertThat(resultBlank).isNotNull().isEmpty();
            verify(residentRepository, never()).findActiveResidentsByLastName(anyString());
        }
    }
}
