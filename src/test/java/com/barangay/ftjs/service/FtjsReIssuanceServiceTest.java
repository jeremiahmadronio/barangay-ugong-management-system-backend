package com.barangay.ftjs.service;

import com.barangay.ftjs.dto.FtjsTimelineDto;
import com.barangay.ftjs.dto.ReIssuanceHistoryDto;
import com.barangay.ftjs.dto.ReIssuanceRequestDto;
import com.barangay.ftjs.model.FirstTimeJobSeeker;
import com.barangay.ftjs.model.FirstTimeJobSeekerAffidavitOfLoss;
import com.barangay.ftjs.model.FirstTimeJobSeekerTimeLine;
import com.barangay.ftjs.repository.FirstTimeJobSeekerAffidavitOfLossRepository;
import com.barangay.ftjs.repository.FirstTimeJobSeekerRepository;
import com.barangay.ftjs.repository.FirstTimeJobSeekerTimeLineRepository;
import com.barangay.people.person.model.Person;
import com.barangay.people.resident.model.Resident;
import com.barangay.usermanagement.user.repository.UserRepository;
import com.barangay.zshared.enumerated.FtjsStatus;
import com.barangay.zshared.enumerated.TimeLineType;
import com.barangay.zshared.exception.ResourceNotFoundException;
import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FtjsReIssuanceServiceTest {

    @Mock
    private FirstTimeJobSeekerRepository firstTimeJobSeekerRepository;

    @Mock
    private FirstTimeJobSeekerAffidavitOfLossRepository firstTimeJobSeekerAffidavitOfLossRepository;

    @Mock
    private FirstTimeJobSeekerTimeLineRepository firstTimeJobSeekerTimeLineRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FtjsReIssuanceService ftjsReIssuanceService;

    @Nested
    @DisplayName("createReIssuanceRequest Method Tests")
    class CreateReIssuanceRequestTests {

        @Test
        @DisplayName("shouldCreateReIssuanceRequest_whenValidFtjsRecord")
        void shouldCreateReIssuanceRequest_whenValidFtjsRecord() throws BadRequestException {
            // Given
            Long ftjsId = 1L;
            FirstTimeJobSeeker ftjs = new FirstTimeJobSeeker();
            ftjs.setId(ftjsId);
            ftjs.setDateSubmitted(LocalDate.now().minusDays(10));
            ftjs.setIssuanceCount(1);
            ftjs.setStatus(FtjsStatus.ISSUED);

            ReIssuanceRequestDto request = new ReIssuanceRequestDto(
                    ftjsId, "Lost certificate in fire", LocalDate.now().minusDays(5), "OR-12345", new byte[]{1, 2, 3}
            );

            FirstTimeJobSeekerAffidavitOfLoss savedAffidavit = new FirstTimeJobSeekerAffidavitOfLoss();
            savedAffidavit.setFtjs(ftjs);
            savedAffidavit.setReason(request.reason());
            savedAffidavit.setDateOfLoss(request.dateOfLoss());
            savedAffidavit.setOrNumber(request.orNumber());
            savedAffidavit.setIssuanceNumber(2);
            savedAffidavit.setAffidavitFiles(request.affidavit());

            when(firstTimeJobSeekerRepository.findById(ftjsId)).thenReturn(Optional.of(ftjs));
            when(firstTimeJobSeekerRepository.save(any(FirstTimeJobSeeker.class))).thenReturn(ftjs);
            when(firstTimeJobSeekerAffidavitOfLossRepository.save(any(FirstTimeJobSeekerAffidavitOfLoss.class))).thenReturn(savedAffidavit);

            // When
            ReIssuanceRequestDto response = ftjsReIssuanceService.createReIssuanceRequest(ftjsId, request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.reason()).isEqualTo("Lost certificate in fire");
            assertThat(response.orNumber()).isEqualTo("OR-12345");
            verify(firstTimeJobSeekerRepository, times(1)).save(ftjs);
            verify(firstTimeJobSeekerAffidavitOfLossRepository, times(1)).save(any(FirstTimeJobSeekerAffidavitOfLoss.class));
            verify(firstTimeJobSeekerTimeLineRepository, times(1)).save(any(FirstTimeJobSeekerTimeLine.class));
        }

        @Test
        @DisplayName("shouldThrowResourceNotFoundException_whenFtjsNotFound")
        void shouldThrowResourceNotFoundException_whenFtjsNotFound() {
            // Given
            Long ftjsId = 99L;
            ReIssuanceRequestDto request = new ReIssuanceRequestDto(
                    ftjsId, "Reason", LocalDate.now(), "OR-1", null
            );
            when(firstTimeJobSeekerRepository.findById(ftjsId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> ftjsReIssuanceService.createReIssuanceRequest(ftjsId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("ftjs not found");
        }
    }

    @Nested
    @DisplayName("getReIssuanceHistory Method Tests")
    class GetReIssuanceHistoryTests {

        @Test
        @DisplayName("shouldReturnHistoryList_whenFtjsExists")
        void shouldReturnHistoryList_whenFtjsExists() {
            // Given
            Long ftjsId = 1L;
            Person person = new Person();
            person.setFirstName("Pedro");
            person.setLastName("Penduko");

            Resident resident = new Resident();
            resident.setPerson(person);

            FirstTimeJobSeeker ftjs = new FirstTimeJobSeeker();
            ftjs.setId(ftjsId);
            ftjs.setResident(resident);

            FirstTimeJobSeekerAffidavitOfLoss affidavit = new FirstTimeJobSeekerAffidavitOfLoss();
            affidavit.setId(10L);
            affidavit.setFtjs(ftjs);
            affidavit.setIssuanceNumber(2);
            affidavit.setDateOfLoss(LocalDate.now().minusDays(5));
            affidavit.setReason("Damaged copy");
            affidavit.setOrNumber("OR-99");

            when(firstTimeJobSeekerRepository.existsById(ftjsId)).thenReturn(true);
            when(firstTimeJobSeekerAffidavitOfLossRepository.findByFtjsIdOrderByIssuanceNumberDesc(ftjsId))
                    .thenReturn(List.of(affidavit));

            // When
            List<ReIssuanceHistoryDto> history = ftjsReIssuanceService.getReIssuanceHistory(ftjsId);

            // Then
            assertThat(history).isNotNull().hasSize(1);
            assertThat(history.get(0).issuanceNumber()).isEqualTo(2);
            assertThat(history.get(0).reason()).isEqualTo("Damaged copy");
            assertThat(history.get(0).requestedBy()).contains("Pedro Penduko");
            verify(firstTimeJobSeekerAffidavitOfLossRepository, times(1)).findByFtjsIdOrderByIssuanceNumberDesc(ftjsId);
        }
    }

    @Nested
    @DisplayName("getFtjsTimeline Method Tests")
    class GetFtjsTimelineTests {

        @Test
        @DisplayName("shouldReturnTimelineEvents_whenFtjsExists")
        void shouldReturnTimelineEvents_whenFtjsExists() {
            // Given
            Long ftjsId = 1L;
            FirstTimeJobSeekerTimeLine timeline = new FirstTimeJobSeekerTimeLine();
            timeline.setId(5L);
            timeline.setTitle("Certificate Re-issued");
            timeline.setDescription("Re-issued FTJS Certificate.");
            timeline.setTimelineType(TimeLineType.RE_ISSUANCE);
            timeline.setEventDate(LocalDateTime.now());

            when(firstTimeJobSeekerRepository.existsById(ftjsId)).thenReturn(true);
            when(firstTimeJobSeekerTimeLineRepository.findByFtjsIdOrderByEventDateDesc(ftjsId))
                    .thenReturn(List.of(timeline));

            // When
            List<FtjsTimelineDto> result = ftjsReIssuanceService.getFtjsTimeline(ftjsId);

            // Then
            assertThat(result).isNotNull().hasSize(1);
            assertThat(result.get(0).title()).isEqualTo("Certificate Re-issued");
            verify(firstTimeJobSeekerTimeLineRepository, times(1)).findByFtjsIdOrderByEventDateDesc(ftjsId);
        }
    }
}
