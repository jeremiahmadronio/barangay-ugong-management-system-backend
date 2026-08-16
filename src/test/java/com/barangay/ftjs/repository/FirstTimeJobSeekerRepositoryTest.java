package com.barangay.ftjs.repository;

import com.barangay.ftjs.model.FirstTimeJobSeeker;
import com.barangay.zshared.enumerated.FtjsStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class FirstTimeJobSeekerRepositoryTest {

    @Autowired
    private FirstTimeJobSeekerRepository ftjsRepository;

    @Nested
    @DisplayName("Counting Queries Tests")
    class CountingQueriesTests {

        @Test
        @DisplayName("shouldCountTotalIssued_whenIssuedRecordsExist")
        void shouldCountTotalIssued_whenIssuedRecordsExist() {
            // Given
            FirstTimeJobSeeker ftjs = new FirstTimeJobSeeker();
            ftjs.setStatus(FtjsStatus.ISSUED);
            ftjs.setFtjsNumber("2026-FTJS-9001");
            ftjs.setDateSubmitted(LocalDate.now());
            ftjs.setIssuanceCount(1);
            ftjs.setIsArchived(false);
            ftjs.setDeleted(false);
            ftjsRepository.save(ftjs);

            // When
            long count = ftjsRepository.countTotalIssued();

            // Then
            assertThat(count).isGreaterThanOrEqualTo(1L);
        }

        @Test
        @DisplayName("shouldCountIssuedBetween_whenDatesMatch")
        void shouldCountIssuedBetween_whenDatesMatch() {
            // Given
            LocalDate today = LocalDate.now();
            FirstTimeJobSeeker ftjs = new FirstTimeJobSeeker();
            ftjs.setStatus(FtjsStatus.ISSUED);
            ftjs.setFtjsNumber("2026-FTJS-9008");
            ftjs.setDateSubmitted(today);
            ftjs.setDeleted(false);
            ftjsRepository.save(ftjs);

            // When
            long count = ftjsRepository.countIssuedBetween(today.minusDays(1), today.plusDays(1));

            // Then
            assertThat(count).isGreaterThanOrEqualTo(1L);
        }

        @Test
        @DisplayName("shouldCountOriginalIssuances_whenIssuanceCountIsOneOrNull")
        void shouldCountOriginalIssuances_whenIssuanceCountIsOneOrNull() {
            // Given
            FirstTimeJobSeeker ftjs = new FirstTimeJobSeeker();
            ftjs.setStatus(FtjsStatus.ISSUED);
            ftjs.setFtjsNumber("2026-FTJS-9002");
            ftjs.setDateSubmitted(LocalDate.now());
            ftjs.setIssuanceCount(1);
            ftjs.setDeleted(false);
            ftjsRepository.save(ftjs);

            // When
            long count = ftjsRepository.countOriginalIssuances();

            // Then
            assertThat(count).isGreaterThanOrEqualTo(1L);
        }

        @Test
        @DisplayName("shouldCountReIssuances_whenIssuanceCountGreaterThanOne")
        void shouldCountReIssuances_whenIssuanceCountGreaterThanOne() {
            // Given
            FirstTimeJobSeeker ftjs = new FirstTimeJobSeeker();
            ftjs.setStatus(FtjsStatus.RE_ISSUANCE);
            ftjs.setFtjsNumber("2026-FTJS-9003");
            ftjs.setDateSubmitted(LocalDate.now());
            ftjs.setIssuanceCount(2);
            ftjs.setDeleted(false);
            ftjsRepository.save(ftjs);

            // When
            long count = ftjsRepository.countReIssuances();

            // Then
            assertThat(count).isGreaterThanOrEqualTo(1L);
        }

        @Test
        @DisplayName("shouldCountByIsArchivedTrue_whenArchivedRecordsExist")
        void shouldCountByIsArchivedTrue_whenArchivedRecordsExist() {
            // Given
            FirstTimeJobSeeker ftjs = new FirstTimeJobSeeker();
            ftjs.setStatus(FtjsStatus.RE_ISSUANCE);
            ftjs.setFtjsNumber("2026-FTJS-9004");
            ftjs.setIsArchived(true);
            ftjs.setDeleted(false);
            ftjsRepository.save(ftjs);

            // When
            long count = ftjsRepository.countByIsArchivedTrue();

            // Then
            assertThat(count).isGreaterThanOrEqualTo(1L);
        }

        @Test
        @DisplayName("shouldCountArchiveBetween_whenDatesMatch")
        void shouldCountArchiveBetween_whenDatesMatch() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            FirstTimeJobSeeker ftjs = new FirstTimeJobSeeker();
            ftjs.setStatus(FtjsStatus.ISSUED);
            ftjs.setFtjsNumber("2026-FTJS-9009");
            ftjs.setIsArchived(true);
            ftjs.setArchivedAt(now);
            ftjs.setDeleted(false);
            ftjsRepository.save(ftjs);

            // When
            long count = ftjsRepository.countArchiveBetween(now.minusDays(1), now.plusDays(1));

            // Then
            assertThat(count).isGreaterThanOrEqualTo(1L);
        }

        @Test
        @DisplayName("shouldCountArchiveOriginalIssuance_whenArchivedAndIssuanceCountIsOne")
        void shouldCountArchiveOriginalIssuance_whenArchivedAndIssuanceCountIsOne() {
            // Given
            FirstTimeJobSeeker ftjs = new FirstTimeJobSeeker();
            ftjs.setStatus(FtjsStatus.ISSUED);
            ftjs.setFtjsNumber("2026-FTJS-9010");
            ftjs.setIsArchived(true);
            ftjs.setIssuanceCount(1);
            ftjs.setDeleted(false);
            ftjsRepository.save(ftjs);

            // When
            long count = ftjsRepository.countArchiveOriginalIssuance();

            // Then
            assertThat(count).isGreaterThanOrEqualTo(1L);
        }

        @Test
        @DisplayName("shouldCountTotalRecentDelete_whenDeletedRecordsExist")
        void shouldCountTotalRecentDelete_whenDeletedRecordsExist() {
            // Given
            FirstTimeJobSeeker ftjs = new FirstTimeJobSeeker();
            ftjs.setStatus(FtjsStatus.ISSUED);
            ftjs.setFtjsNumber("2026-FTJS-9005");
            ftjs.setDeleted(true);
            ftjs.setDeletedAt(LocalDateTime.now());
            ftjsRepository.save(ftjs);

            // When
            long count = ftjsRepository.countTotalRecentDelete();

            // Then
            assertThat(count).isGreaterThanOrEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("Native Modifying Queries Tests")
    class NativeModifyingQueriesTests {

        @Test
        @DisplayName("shouldArchiveRecordNative_whenIdExists")
        void shouldArchiveRecordNative_whenIdExists() {
            // Given
            FirstTimeJobSeeker ftjs = new FirstTimeJobSeeker();
            ftjs.setStatus(FtjsStatus.ISSUED);
            ftjs.setFtjsNumber("2026-FTJS-9006");
            ftjs.setIsArchived(false);
            ftjs.setDeleted(false);
            FirstTimeJobSeeker saved = ftjsRepository.save(ftjs);

            // When
            int affected = ftjsRepository.archiveRecordNative(saved.getId());

            // Then
            assertThat(affected).isEqualTo(1);
        }

        @Test
        @DisplayName("shouldRestoreRecordNative_whenIdExists")
        void shouldRestoreRecordNative_whenIdExists() {
            // Given
            FirstTimeJobSeeker ftjs = new FirstTimeJobSeeker();
            ftjs.setStatus(FtjsStatus.RE_ISSUANCE);
            ftjs.setFtjsNumber("2026-FTJS-9007");
            ftjs.setIsArchived(true);
            ftjs.setDeleted(false);
            FirstTimeJobSeeker saved = ftjsRepository.save(ftjs);

            // When
            int affected = ftjsRepository.restoreRecordNative(saved.getId());

            // Then
            assertThat(affected).isEqualTo(1);
        }
    }
}
