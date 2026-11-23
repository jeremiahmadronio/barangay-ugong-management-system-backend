package com.barangay.zshared.scheduler;

import com.barangay.clearance.model.IssuedCertificate;
import com.barangay.clearance.repository.IssuedCertificateRepository;
import com.barangay.clearance.service.ClearanceArchive;
import com.barangay.zshared.entity.ArchiveSettings;
import com.barangay.zshared.enumerated.ClearanceStatus;
import com.barangay.zshared.repository.ArchiveSettingsRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * BDD Unit tests for {@link ClearanceScheduler}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClearanceScheduler BDD Tests")
class ClearanceSchedulerTest {

    @Mock
    private ArchiveSettingsRepository archiveSettingsRepository;

    @Mock
    private IssuedCertificateRepository issuedCertificateRepository;

    @Mock
    private ClearanceArchive clearanceArchive;

    @InjectMocks
    private ClearanceScheduler scheduler;

    @Nested
    @DisplayName("Trash Retention Tests")
    class TrashRetentionTests {

        @Test
        @DisplayName("givenAutoDeleteDisabled_whenAutoDeleteExpiredTrashCertificates_thenDoNotPurge")
        void givenAutoDeleteDisabled_whenAutoDeleteExpiredTrashCertificates_thenDoNotPurge() {
            // Given
            ArchiveSettings settings = new ArchiveSettings();
            settings.setAutoDeleteEnabled(false);
            given(archiveSettingsRepository.findById(1L)).willReturn(Optional.of(settings));

            // When
            scheduler.autoDeleteExpiredTrashCertificates();

            // Then
            then(issuedCertificateRepository).should(never()).findExpiredSoftDeletedCertificates(any());
            then(clearanceArchive).should(never()).permanentDeleteCertificate(any());
        }

        @Test
        @DisplayName("givenExpiredTrashCertificates_whenAutoDeleteExpiredTrashCertificates_thenPurgeViaClearanceArchive")
        void givenExpiredTrashCertificates_whenAutoDeleteExpiredTrashCertificates_thenPurgeViaClearanceArchive() {
            // Given
            ArchiveSettings settings = new ArchiveSettings();
            settings.setAutoDeleteEnabled(true);
            settings.setDeleteAfterDays(30);
            given(archiveSettingsRepository.findById(1L)).willReturn(Optional.of(settings));

            IssuedCertificate cert = new IssuedCertificate();
            cert.setId(55L);
            cert.setCertNumber("CLR-2026-0055");
            given(issuedCertificateRepository.findExpiredSoftDeletedCertificates(any(LocalDateTime.class)))
                    .willReturn(List.of(cert));

            // When
            scheduler.autoDeleteExpiredTrashCertificates();

            // Then
            then(clearanceArchive).should().permanentDeleteCertificate(55L);
        }
    }

    @Nested
    @DisplayName("Active Certificate Expiration Tests")
    class ActiveCertificateExpirationTests {

        @Test
        @DisplayName("givenActiveCertificatesPastExpiry_whenAutoExpireActiveCertificates_thenUpdateStatusToExpired")
        void givenActiveCertificatesPastExpiry_whenAutoExpireActiveCertificates_thenUpdateStatusToExpired() {
            // Given
            IssuedCertificate expiredCert = new IssuedCertificate();
            expiredCert.setId(88L);
            expiredCert.setCertNumber("CLR-2026-0088");
            expiredCert.setStatus(ClearanceStatus.ACTIVE);
            expiredCert.setExpiryDate(LocalDate.now().minusDays(2));

            given(issuedCertificateRepository.findAllActiveCertificates()).willReturn(List.of(expiredCert));

            // When
            scheduler.autoExpireActiveCertificates();

            // Then
            assertThat(expiredCert.getStatus()).isEqualTo(ClearanceStatus.EXPIRED);
            then(issuedCertificateRepository).should().save(expiredCert);
        }
    }
}
