package com.barangay.people.officer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.barangay.people.officer.model.Officer;
import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.person.model.Person;
import com.barangay.usermanagement.department.model.Department;
import com.barangay.zshared.enumerated.OfficerStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArchiveOfficerService BDD Unit Tests")
class ArchiveOfficerServiceTest {

    @Mock
    private OfficerRepository officerRepository;

    @InjectMocks
    private ArchiveOfficerService archiveOfficerService;

    private Officer createSampleOfficer(Long id, String first, String last, String position) {
        Person p = new Person();
        p.setId(100L + id);
        p.setFirstName(first);
        p.setLastName(last);

        Department d = new Department();
        d.setId(10L);
        d.setName("ADMIN");

        Officer o = new Officer();
        o.setId(id);
        o.setPerson(p);
        o.setDepartment(d);
        o.setPosition(position);
        o.setStatus(OfficerStatus.ARCHIVED);
        return o;
    }

    @Nested
    @DisplayName("getArchivedOfficers Method Tests")
    class GetArchivedOfficersTests {

        @Test
        @DisplayName("shouldReturnArchivedOfficers_whenArchivedRecordsExist")
        void shouldReturnArchivedOfficers_whenArchivedRecordsExist() {
            // Given
            Officer o1 = createSampleOfficer(1L, "Cardo", "Dalisay", "Desk Officer");
            when(officerRepository.findAllByStatus(OfficerStatus.ARCHIVED)).thenReturn(List.of(o1));

            // When
            List<Map<String, Object>> result = archiveOfficerService.getArchivedOfficers("Cardo");

            // Then
            assertThat(result).isNotNull().hasSize(1);
            assertThat(result.get(0).get("employee")).isEqualTo("Cardo Dalisay");
            assertThat(result.get(0).get("department")).isEqualTo("OVERALL");
            assertThat(result.get(0).get("status")).isEqualTo("ARCHIVED");
            verify(officerRepository, times(1)).findAllByStatus(OfficerStatus.ARCHIVED);
        }
    }
}
