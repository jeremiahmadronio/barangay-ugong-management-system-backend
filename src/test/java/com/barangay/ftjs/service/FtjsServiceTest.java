package com.barangay.ftjs.service;

import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.ftjs.dto.*;
import com.barangay.ftjs.model.FirstTimeJobSeeker;
import com.barangay.ftjs.repository.FirstTimeJobSeekerRepository;
import com.barangay.ftjs.repository.FirstTimeJobSeekerTimeLineRepository;
import com.barangay.people.person.model.Person;
import com.barangay.people.resident.model.Resident;
import com.barangay.people.resident.repository.ResidentRepository;
import com.barangay.usermanagement.user.repository.UserRepository;
import com.barangay.zshared.enumerated.FtjsStatus;
import com.barangay.zshared.exception.ResourceNotFoundException;
import com.barangay.zshared.mapper.PersonMapper;
import com.barangay.zshared.mapper.ResidentMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FtjsServiceTest {

    @Mock
    private FirstTimeJobSeekerRepository ftjsRepository;

    @Mock
    private FirstTimeJobSeekerTimeLineRepository timeLineRepository;

    @Mock
    private ResidentRepository residentRepository;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ResidentMapper residentMapper;

    @Mock
    private PersonMapper personMapper;

    @InjectMocks
    private FtjsService ftjsService;

    @Nested
    @DisplayName("getStats Method Tests")
    class GetStatsTests {

        @Test
        @DisplayName("shouldReturnStatsResponse_whenRepositoryReturnsCounts")
        void shouldReturnStatsResponse_whenRepositoryReturnsCounts() {
            // Given
            when(ftjsRepository.countTotalIssued()).thenReturn(100L);
            when(ftjsRepository.countIssuedBetween(any(), any())).thenReturn(10L).thenReturn(5L);
            when(ftjsRepository.countOriginalIssuances()).thenReturn(80L);
            when(ftjsRepository.countReIssuances()).thenReturn(20L);

            // When
            FtjsStatsResponse response = ftjsService.getStats();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.totalCertificatesIssued()).isEqualTo(100L);
            assertThat(response.totalCertificatedThisMonth()).isEqualTo(10L);
            assertThat(response.originalIssuances()).isEqualTo(80L);
            assertThat(response.reIssuances()).isEqualTo(20L);
            assertThat(response.issuedThisMonthTrendPercentage()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("shouldHandleZeroLastMonth_whenCalculatingTrend")
        void shouldHandleZeroLastMonth_whenCalculatingTrend() {
            // Given
            when(ftjsRepository.countTotalIssued()).thenReturn(50L);
            when(ftjsRepository.countIssuedBetween(any(), any())).thenReturn(0L).thenReturn(0L);
            when(ftjsRepository.countOriginalIssuances()).thenReturn(40L);
            when(ftjsRepository.countReIssuances()).thenReturn(10L);

            // When
            FtjsStatsResponse response = ftjsService.getStats();

            // Then
            assertThat(response.issuedThisMonthTrendPercentage()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("getSummaryTable Method Tests")
    class GetSummaryTableTests {

        @Test
        @DisplayName("shouldReturnPaginatedSummary_whenFiltered")
        void shouldReturnPaginatedSummary_whenFiltered() {
            // Given
            PageRequest pageable = PageRequest.of(0, 5);
            FtjsSummaryResponse summary = new FtjsSummaryResponse(
                    1L, "2026-FTJS-0001", "Juan Dela Cruz", 1L, FtjsStatus.ISSUED, "Employment", LocalDateTime.now()
            );
            Page<FtjsSummaryResponse> expectedPage = new PageImpl<>(List.of(summary));

            when(ftjsRepository.findSummaryFiltered(eq("Juan"), eq(FtjsStatus.ISSUED), eq("Employment"), eq(1), eq(pageable)))
                    .thenReturn(expectedPage);

            // When
            Page<FtjsSummaryResponse> result = ftjsService.getSummaryTable("Juan", FtjsStatus.ISSUED, "Employment", 1, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).trackingNumber()).isEqualTo("2026-FTJS-0001");
            verify(ftjsRepository, times(1)).findSummaryFiltered("Juan", FtjsStatus.ISSUED, "Employment", 1, pageable);
        }
    }

    @Nested
    @DisplayName("getResidentFtjsProfile Method Tests")
    class GetResidentFtjsProfileTests {

        @Test
        @DisplayName("shouldReturnFtjsResidentSearchResponse_whenResidentExists")
        void shouldReturnFtjsResidentSearchResponse_whenResidentExists() {
            // Given
            Long residentId = 1L;
            Person person = new Person();
            person.setFirstName("Juan");
            person.setLastName("Dela Cruz");

            Resident resident = new Resident();
            resident.setPerson(person);

            when(residentRepository.findById(residentId)).thenReturn(Optional.of(resident));

            // When
            FtjsResidentSearchResponse response = ftjsService.getResidentFtjsProfile(residentId);

            // Then
            assertThat(response).isNotNull();
            verify(residentRepository, times(1)).findById(residentId);
        }

        @Test
        @DisplayName("shouldThrowResourceNotFoundException_whenResidentDoesNotExist")
        void shouldThrowResourceNotFoundException_whenResidentDoesNotExist() {
            // Given
            Long residentId = 999L;
            when(residentRepository.findById(residentId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> ftjsService.getResidentFtjsProfile(residentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Resident not found with id: 999");
        }
    }
}
