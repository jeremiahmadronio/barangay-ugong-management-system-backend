package com.barangay.people.resident.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.barangay.people.person.model.Person;
import com.barangay.people.resident.model.Resident;
import com.barangay.people.resident.repository.ResidentRepository;
import com.barangay.zshared.enumerated.ResidentStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArchiveResidentService BDD Unit Tests")
class ArchiveResidentServiceTest {

    @Mock
    private ResidentRepository residentRepository;

    @InjectMocks
    private ArchiveResidentService archiveResidentService;

    private Resident createSampleResident(Long residentId, String firstName, String lastName, ResidentStatus status) {
        Person p = new Person();
        p.setId(100L + residentId);
        p.setFirstName(firstName);
        p.setLastName(lastName);
        p.setContactNumber("09123456789");

        Resident r = new Resident();
        r.setResidentId(residentId);
        r.setPerson(p);
        r.setBarangayIdNumber("BRGY-2026-" + residentId);
        r.setStatus(status);
        return r;
    }

    @Nested
    @DisplayName("getArchivedResidents Method Tests")
    class GetArchivedResidentsTests {

        @Test
        @DisplayName("shouldReturnArchivedResidents_whenArchivedRecordsExist")
        void shouldReturnArchivedResidents_whenArchivedRecordsExist() {
            // Given
            Resident r1 = createSampleResident(1L, "Juan", "Dela Cruz", ResidentStatus.ARCHIVED);
            when(residentRepository.findAllByStatus(ResidentStatus.ARCHIVED)).thenReturn(List.of(r1));

            // When
            List<Map<String, Object>> result = archiveResidentService.getArchivedResidents("Juan");

            // Then
            assertThat(result).isNotNull().hasSize(1);
            assertThat(result.get(0).get("fullName")).isEqualTo("Juan Dela Cruz");
            assertThat(result.get(0).get("status")).isEqualTo("ARCHIVED");
            verify(residentRepository, times(1)).findAllByStatus(ResidentStatus.ARCHIVED);
        }
    }

    @Nested
    @DisplayName("deleteResidentPermanent Method Tests")
    class DeleteResidentPermanentTests {

        @Test
        @DisplayName("shouldPermanentDeleteResident_whenResidentExists")
        void shouldPermanentDeleteResident_whenResidentExists() {
            // Given
            Long residentId = 1L;
            Resident resident = createSampleResident(residentId, "Juan", "Dela Cruz", ResidentStatus.ARCHIVED);
            when(residentRepository.findById(residentId)).thenReturn(Optional.of(resident));

            // When
            archiveResidentService.deleteResidentPermanent(residentId);

            // Then
            assertThat(resident.getIsPermanentDeleted()).isTrue();
            verify(residentRepository, times(1)).save(resident);
        }
    }

    @Nested
    @DisplayName("getTrashResidents Method Tests")
    class GetTrashResidentsTests {

        @Test
        @DisplayName("shouldReturnTrashResidents_whenSoftDeletedResidentsExist")
        void shouldReturnTrashResidents_whenSoftDeletedResidentsExist() {
            // Given
            Resident r1 = createSampleResident(1L, "Juan", "Dela Cruz", ResidentStatus.INACTIVE);
            r1.softDelete();
            when(residentRepository.findAll()).thenReturn(List.of(r1));

            // When
            List<Map<String, Object>> result = archiveResidentService.getTrashResidents("");

            // Then
            assertThat(result).isNotNull().hasSize(1);
            assertThat(result.get(0).get("fullName")).isEqualTo("Juan Dela Cruz");
            assertThat(result.get(0).get("status")).isEqualTo("DELETED");
            verify(residentRepository, times(1)).findAll();
        }
    }
}
